package tabos.ui

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.MenuBar

class MenuController implements Initializable {
    @FXML private MenuBar menuBar

    @Override
    void initialize(URL location, ResourceBundle resources) {
        menuBar.useSystemMenuBarProperty().set(true)
    }

    @FXML private void handleNew() {
        println("New file")
    }

    @FXML private void handleOpen() {
        println("Open file")
    }

    @FXML private void handleSave() {
        println("Save file")
    }

    @FXML private void handleExit() {
        Platform.exit()
    }

    @FXML private void handleUndo() {
        println("Undo")
    }

    @FXML private void handleRedo() {
        println("Redo")
    }

    @FXML private void handleCut() {
        println("Cut")
    }

    @FXML private void handleCopy() {
        println("Copy")
    }

    @FXML private void handlePaste() {
        println("Paste")
    }
}
