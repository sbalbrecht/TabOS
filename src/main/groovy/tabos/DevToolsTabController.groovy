package tabos

import groovy.json.JsonOutput
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.text.Font
import javafx.scene.text.Text

class DevToolsTabController implements Initializable {
    @FXML private Text settings
    private StringProperty settingsProperty

    @Override
    void initialize(URL location, ResourceBundle resources) {
        settingsProperty = new SimpleStringProperty(JsonOutput.prettyPrint(JsonOutput.toJson(Configurations.get())))
        Configurations.subscriptions.add({ settingsProperty.set(JsonOutput.prettyPrint(JsonOutput.toJson(Configurations.get()))) })
        settings.textProperty().bindBidirectional(settingsProperty)
        settings.font = new Font('Consolas', 12)
    }
}
