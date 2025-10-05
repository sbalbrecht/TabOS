package tabos

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ConfigurationsTests {
    @TempDir
    private File configDir

    @BeforeEach
    void setUp() {
        Configurations.init(configDir)
    }

    @Test
    void works() {
        println 'Initializing configuration...'
        def config = Configurations.get(AppConfig)

        println 'Initial values:'
        println "  Window: ${config.ui.window.width}x${config.ui.window.height}"
        println "  Maximized: ${config.ui.window.maximized}"
        println "  Visible panes: ${config.ui.panes.visible}"
        println "  Active pane: ${config.ui.panes.selected}"
        println "  Left sidebar: ${config.ui.sidebars.left}px"
        println "  Theme: ${config.preferences.theme}"
        println "  Autosave: ${config.preferences.autosave}"
        println()

        println 'Modifying nested configuration...'
        config.ui.window.width = 1920
        config.ui.window.height = 1080
        config.ui.window.maximized = true
        config.ui.sidebars.left = 350
        config.preferences.theme = 'dark'
        config.project.recentFiles = [
            '/home/user/symphony.mxml',
            '/home/user/sonata.mxml'
        ]

        println "  New window: ${config.ui.window.width}x${config.ui.window.height}"
        println "  Maximized: ${config.ui.window.maximized}"
        println "  Left sidebar: ${config.ui.sidebars.left}px"
        println "  Theme: ${config.preferences.theme}"
        println "  Recent files: ${config.project.recentFiles}"
        println()

        config.ui.panes.visible << 'properties'
        println "  Updated visible panes: ${config.ui.panes.visible}"
        println()

        println 'Waiting for debounced save...'
        Thread.sleep(1000)

        Configurations.shutdown()
        Configurations.init(configDir)

        println()
        println '=== Creating new instance to verify persistence ==='
        def config2 = Configurations.get(AppConfig)
        println "  Loaded window: ${config2.ui.window.width}x${config2.ui.window.height}"
        println "  Loaded maximized: ${config2.ui.window.maximized}"
        println "  Loaded theme: ${config2.preferences.theme}"
        println "  Loaded sidebar: ${config2.ui.sidebars.left}px"
        println "  Loaded visible panes: ${config2.ui.panes.visible}"
        println "  Loaded recent files: ${config2.project.recentFiles}"

        Configurations.shutdown()

        println()
        println "=== Generated JSON Structure ==="
        def file = new File(System.getProperty('user.home'), '.tabos/settings.json')
        if (file.exists()) {
            println file.text
        }

        assert config2.ui.window.width == 1920
        assert config2.ui.window.height == 1080
    }
}