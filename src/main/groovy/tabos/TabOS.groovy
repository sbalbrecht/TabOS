package tabos

import groovy.util.logging.Slf4j
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Parent

import static tabos.Loader.load

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

@Slf4j
class TabOS extends Application {
    private static final AppConfig config = Configurations.get()
    static Map<ObservableValue, List<ChangeListener>> subscriptions = [:]

    static void main(String[] args) {
        launch(TabOS, args)
    }

    @Override
    void start(Stage stage) {
        stage.title = 'TabOS'
        stage.scene = new Scene(load('/MainView.fxml'), config.ui.window.width, config.ui.window.height).tap {
            ChangeListener widthListener = (o, oldVal, newVal) -> config.ui.window.width = Math.max(newVal as int, 0)
            widthProperty().addListener widthListener
            subscriptions.get(widthProperty(), []) << widthListener

            ChangeListener heightListener = (o, oldVal, newVal) -> config.ui.window.height = Math.max(newVal as int, 0)
            heightProperty().addListener heightListener
            subscriptions.get(heightProperty(), []) << widthListener
        }
        stage.show()
    }

    @Override
    void stop() {
        subscriptions.each { property, listeners ->
            listeners.each { property.removeListener it }
        }.clear()
    }
}

class Loader {
    static Parent load(String fxml) {
        new FXMLLoader(TabOS.getResource(fxml)).load()
    }
}