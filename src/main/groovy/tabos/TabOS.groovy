package tabos

import com.tangorabox.componentinspector.fx.FXComponentInspectorHandler

import static tabos.io.Loader.load

import groovy.util.logging.Slf4j
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import tabos.config.AppConfig
import tabos.config.Configurations
import javafx.application.Application
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
        FXComponentInspectorHandler.handleAll() // todo if devtools enabled
    }

    @Override
    void stop() {
        subscriptions.each { property, listeners ->
            listeners.each { property.removeListener it }
        }.clear()
        Configurations.shutdown()
    }
}
