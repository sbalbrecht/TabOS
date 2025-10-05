package tabos.io

import javafx.fxml.FXMLLoader
import javafx.scene.Parent

class Loader {
    static Parent load(String fxml) {
        new FXMLLoader(Loader.getResource(fxml)).load()
    }
}
