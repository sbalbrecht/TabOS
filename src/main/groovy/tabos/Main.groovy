package tabos

import static tabos.Loader.load

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class Main extends Application {
    static void main(String[] args) {
        launch(Main, args)
    }

    @Override
    void start(Stage stage) {
        stage.title = 'TabOS'
        stage.scene = new Scene(load('MainView.fxml'), 1200, 800)
        stage.show()
    }
}

class Loader {
    static <T> T load(String fxml) {
        new FXMLLoader(Main.getResource(fxml)).load()
    }
}