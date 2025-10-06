package tabos.ui

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.TextArea
import javafx.scene.control.TreeView
import javafx.scene.input.MouseEvent
import javafx.scene.text.Font
import tabos.config.Configurations

@Slf4j
class DevToolsTabController implements Initializable {
    @FXML private TextArea settings
    @FXML private TreeView elementsTree
    private boolean elementSelectorEnabled = false

    @Override
    void initialize(URL location, ResourceBundle resources) {
        settings.setFont Font.font(Constants.FONT_MONOSPACE, 12)
        settings.setText JsonOutput.prettyPrint(JsonOutput.toJson(Configurations.get()))
        Configurations.subscriptions.add({ settings.setText JsonOutput.prettyPrint(JsonOutput.toJson(Configurations.get())) })
    }

    void toggleElementSelector(MouseEvent event) {
        elementSelectorEnabled = !elementSelectorEnabled
        log.info "Element selector enabled = $elementSelectorEnabled"
    }
}
