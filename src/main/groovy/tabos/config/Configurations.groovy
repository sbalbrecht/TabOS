package tabos.config

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

import java.util.concurrent.*
import java.lang.reflect.*

@Slf4j
class Configurations {
    private static final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor()
    private static final propertyTypes = [int, Integer, long, Long, double, Double, float, Float, boolean, Boolean, short, Short, byte, Byte, List, Map, String, Enum]
    private static final Map<Class, ConfigurationContext> contextsByClass = [:]
    private static ConfigurationContext rootContext
    static final List subscriptions = []
    static File configDir
    static File configFile

    static <T> T get(Class<T> configClass = AppConfig) {
        if (contextsByClass.isEmpty()) init()
        contextsByClass[configClass].instance as T
    }

    static ConfigurationContext init(
        File configDir = new File((System.properties.'tabos.home' ?: System.properties.'user.home') as String),
        String configFileName = '.tabos.json'
    ) {
        this.configDir = configDir
        configFile = new File(Configurations.configDir, configFileName)
        buildContexts().tap {
            rootContext = it
            load()
            allContexts.each { ConfigurationContext ctx ->
                ctx.instance.metaClass.setProperty = { String name, value ->
                    if (ctx.properties.containsKey(name)) {
                        ctx.properties[name].field.set(ctx.instance, value)
                        subscriptions.each { it() }
                        rootContext?.debouncedSave()
                    } else {
                        ctx.instance.metaClass.setProperty(ctx.instance, name, value)
                    }
                }
            }
        }
    }

    private static ConfigurationContext buildContexts(
        Object instance = new AppConfig(),
        ConfigurationContext context = new ConfigurationContext(),
        List<String> namespace = []
    ) {
        context.instance = instance
        contextsByClass[instance.class] = context
        instance.class.declaredFields.findAll { !it.synthetic }.each { field ->
            field.accessible = true
            if (field.type in propertyTypes) {
                context.properties[field.name] = new PropertyInfo().tap {
                    it.field = field
                    it.path = namespace ? namespace + [field.name] : [field.name]
                }
            } else if (field.type.getAnnotation(Config)) {
                def nestedInstance = field.type.getDeclaredConstructor().newInstance()
                def nestedContext = new ConfigurationContext(parent: context)
                def fullPath = namespace ? namespace + [field.name] : [field.name]
                field.set(instance, nestedInstance)
                context.children[field.name] = nestedContext
                buildContexts(nestedInstance, nestedContext, fullPath)
            }
        }
        context
    }

    static void save() {
        if (!configDir.exists()) configDir.mkdirs()
        try {
            configFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(rootContext.instance))
            log.info "Saved config to $configFile"
        } catch (Exception e) {
            log.error "Error saving configuration to $configFile", e
        }
    }

    static void load() {
        if (!configFile.exists()) save()
        try {
            rootContext.loadFromJson(new JsonSlurper().parse(configFile) as Map)
        } catch (Exception e) {
            log.warn "Failed to load configuration from $configFile. Using default values.", e
        }
    }

    static void shutdown() {
        subscriptions.clear()
        rootContext?.pendingSave?.cancel(false)
        saveExecutor.shutdown()
        rootContext = null
    }

    private static class ConfigurationContext {
        Object instance
        ConfigurationContext parent
        Map<String, ConfigurationContext> children = [:]
        Map<String, PropertyInfo> properties = [:]
        ScheduledFuture<?> pendingSave

        List<ConfigurationContext> getAllContexts() {
            [this, *children.values()*.allContexts.flatten()]
        }

        void loadFromJson(Map json) {
            properties.each { fieldName, propInfo ->
                def value = getNestedValue(json, propInfo.path)
                if (value != null) {
                    propInfo.field.set(instance, convertValue(value, propInfo.field.type))
                }
            }
            children.each { fieldName, childContext ->
                childContext.loadFromJson(json)
            }
        }

        void debouncedSave() {
            if (parent) {
                parent.debouncedSave()
                return
            }
            pendingSave?.cancel(false)
            pendingSave = saveExecutor.schedule(this::save, 500, TimeUnit.MILLISECONDS)
        }

        private static Object getNestedValue(Map map, List parts) {
//            parts.inject(map) { map?[it] }
            def current = map
            for (part in parts) {
                if (current instanceof Map && current.containsKey(part)) {
                    current = current[part]
                } else {
                    return null
                }
            }
            return current
        }

        private static Object convertValue(Object value, Class<?> targetType) {
            if (value == null) return null
            if (targetType.isAssignableFrom(value.class)) return value
            if (targetType in [short, Short, int, Integer]) return value as Integer
            if (targetType in [long, Long]) return value as Long
            if (targetType in [double, Double, float, Float]) return value as Double
            if (targetType in [boolean, Boolean]) return value as Boolean
            if (targetType == String) return value.toString()
            if (List.isAssignableFrom(targetType)) return value as List
            if (Map.isAssignableFrom(targetType)) return value as Map
            return value
        }
    }

    private static class PropertyInfo {
        Field field
        List<String> path = []
    }
}