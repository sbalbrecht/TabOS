package tabos

@Config class AppConfig {
    UiConfig ui = new UiConfig()
    ProjectConfig project = new ProjectConfig()
    PreferencesConfig preferences = new PreferencesConfig()
    boolean firstRun = true
}

@Config class UiConfig {
    UiWindowConfig window = new UiWindowConfig()
    UiPanesConfig panes = new UiPanesConfig()
    UiSidebarsConfig sidebars = new UiSidebarsConfig()
}

@Config class UiWindowConfig {
    int width = 1200
    int height = 800
    boolean maximized = false
    int x = 100
    int y = 100
}

@Config class UiPanesConfig {
    List<String> visible = ['score', 'inspector', 'mixer']
    String selected = 'score'
}

@Config class UiSidebarsConfig {
    double left = 0.2
    int right = 300
    int bottom = 200
}

@Config class ProjectConfig {
    List<String> recentFiles = []
}

@Config class ScoreConfig {
    int scrollPosition = 0
    double zoomLevel = 1.0
}

@Config class PreferencesConfig {
    boolean autosave = true
    int autosaveInterval = 1000 * 10
    String theme = "light"
    String language = "en"
}
