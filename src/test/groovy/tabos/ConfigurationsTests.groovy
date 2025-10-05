package tabos

import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tabos.config.AppConfig
import tabos.config.Configurations
import tabos.config.UiWindowConfig

@Slf4j
class ConfigurationsTests {
    @Test
    void works(@TempDir File configDir) {
        println 'Initializing configuration...'
        Configurations.init(configDir)

        def config = Configurations.get(AppConfig)

        println "Default: ${config.ui.window.width} x ${config.ui.window.height}"
        assert config.ui.window.width == new UiWindowConfig().width
        assert config.ui.window.height == new UiWindowConfig().height

        config.ui.window.width = 1920
        config.ui.window.height = 1080
        config.ui.window.maximized = true
        config.ui.sidebars.left = 0.45
        config.preferences.theme = 'dark'
        config.project.recentFiles = [
            '/home/user/symphony.mxml',
            '/home/user/sonata.mxml'
        ]

        println "Updated: $config.ui.window.width x $config.ui.window.height"
        assert config.ui.window.width == 1920
        assert config.ui.window.height == 1080

        config.ui.panes.visible << 'properties'

        Thread.sleep(1000)

        Configurations.shutdown()
        Configurations.init(configDir)

        def config2 = Configurations.get(AppConfig)
        println "Loaded: ${config2.ui.window.width} x $config2.ui.window.height"
        assert config.ui.window.width == 1920
        assert config.ui.window.height == 1080

        Configurations.shutdown()

        println Configurations.configFile.text
    }
}