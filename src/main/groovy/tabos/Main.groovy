package tabos

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class Main extends Application {
    @Override
    void start(Stage stage) {
        Label label = new Label('Hello world')
        Scene scene = new Scene(new StackPane(label), 640, 480)
        stage.setScene(scene)
        stage.show()
    }

    static void main(String[] args) {
        launch(Main, args)
    }
}
