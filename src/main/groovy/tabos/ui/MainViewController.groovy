package tabos.ui

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.geometry.Orientation
import tabos.config.AppConfig
import tabos.config.Configurations

class MainViewController implements Initializable {
    private static final AppConfig config = Configurations.get()
    @FXML private ToolBar toolbar
    @FXML private TabPane editorTabPane
    @FXML private SplitPane verticalSplitPane
    @FXML private SplitPane horizontalSplitPane
    @FXML private TabPane leftSidebarTabs
    @FXML private TabPane bottomTabs

    @Override
    void initialize(URL location, ResourceBundle resources) {
        horizontalSplitPane.dividers[0].positionProperty().bindBidirectional(BindUtil.prop(config.ui.sidebars, 'left'))
        horizontalSplitPane.dividers[1].positionProperty().bindBidirectional(BindUtil.prop(config.ui.sidebars, 'right'))
        verticalSplitPane.dividers[0].positionProperty().bindBidirectional(BindUtil.prop(config.ui.sidebars, 'bottom'))
    }
}