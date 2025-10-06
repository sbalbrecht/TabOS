package tabos.ui

import groovy.util.logging.Slf4j
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.MenuBar
import javafx.stage.FileChooser
import tabos.Score
import tabos.io.GP5InputStream

@Slf4j
class MenuController implements Initializable {
    @FXML private MenuBar menuBar

    @Override
    void initialize(URL location, ResourceBundle resources) {
        menuBar.useSystemMenuBarProperty().set(true)
    }

    @FXML private void createNewScore() {
        new Score()
    }

    @FXML private void openFile() {
        File file = new FileChooser().with {
            setTitle("Open Score")
            getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GP5", "*.gp5"),
            )
            showOpenDialog(menuBar.getScene().getWindow())
        }
        if (!file?.exists()) {
            log.info "Selected file ${file?.canonicalPath} does not exist"
            return
        }

        String ext = file.name[file.name.lastIndexOf('.')..-1]
        switch (ext) {
            case '.gp5' -> new GP5InputStream(file.newInputStream()).readScore()
            default -> new Alert(Alert.AlertType.ERROR, "File type not supported: $ext").show()
        }
    }

    @FXML void quit() { Platform.exit() }
}
