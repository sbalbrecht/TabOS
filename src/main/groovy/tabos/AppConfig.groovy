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
    int left = 250
    int right = 300
    int bottom = 200
}

@Config class ProjectConfig {
    List<String> recentFiles = []
    int maxRecentFiles = 10
}

@Config class OpenScoreConfig {
    String path = ""
    int scrollPosition = 0
    double zoomLevel = 1.0
}

@Config class PreferencesConfig {
    boolean autosave = true
    int autosaveInterval = 300
    String theme = "light"
    String language = "en"
}
