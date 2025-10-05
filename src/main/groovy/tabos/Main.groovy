package tabos

import groovy.util.logging.Slf4j
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.scene.Parent

import static tabos.Loader.load

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

@Slf4j
class Main extends Application {
    static Map<Property, List<ChangeListener>> subscriptions = [:]

    static void main(String[] args) {
        launch(Main, args)
    }

    @Override
    void start(Stage stage) {
        stage.title = 'TabOS'
        stage.scene = new Scene(load('/MainView.fxml'), 1200, 800)
        stage.show()
    }

    @Override
    void stop() {
        subscriptions.each { property, listeners ->
            listeners.each { property.removeListener it }
        }
    }
}

class Loader {
    static Parent load(String fxml) {
        new FXMLLoader(Main.getResource(fxml)).load()
    }
}