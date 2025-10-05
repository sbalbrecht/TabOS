package tabos

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.geometry.Orientation

class MainViewController implements Initializable {
    private static final AppConfig config = Configurations.get()
    @FXML private MenuBar menuBar
    @FXML private ToolBar toolbar
    @FXML private SplitPane verticalSplitPane
    @FXML private SplitPane horizontalSplitPane
    @FXML private TabPane sidebarTabs
    @FXML private TabPane bottomTabs
    @FXML private TreeView<String> projectTree
    @FXML private VBox propertiesBox
    @FXML private ListView<String> instrumentList
    @FXML private HBox timeline
    @FXML private HBox mixer
    @FXML private VBox scoreArea

    @Override
    void initialize(URL location, ResourceBundle resources) {
        menuBar.useSystemMenuBarProperty().set(true)
        horizontalSplitPane.dividers[0].positionProperty().bindBidirectional(BindUtil.prop(config.ui.sidebars, 'left'))
        horizontalSplitPane.dividers[1].positionProperty().bindBidirectional(BindUtil.prop(config.ui.sidebars, 'right'))
        verticalSplitPane.dividers[0].positionProperty().bindBidirectional(BindUtil.prop(config.ui.sidebars, 'bottom'))

        TreeItem<String> root = new TreeItem<>("Project Root")
        root.expanded = true
        root.children.addAll(
            new TreeItem<>("Songs"),
            new TreeItem<>("Tracks"),
            new TreeItem<>("Instruments")
        )
        projectTree.root = root

        mixer.children.addAll(["Track 1", "Track 2", "Track 3", "Master"].collect { track ->
            new VBox(5,
                new Label(track),
                new Slider(0, 100, 75).tap {
                    it.orientation = Orientation.VERTICAL
                    it.prefHeight = 150
                },
                new Label("75%")
            )
        })

        (1..5).each { trackNum ->
            VBox staff = new VBox(2)
            staff.children.add(new Label("Track $trackNum"))

            (0..4).each {
                Region line = new Region()
                line.prefHeight = 1
                line.style = "-fx-background-color: black;"
                staff.children.add(line)
            }

            scoreArea.children.add(staff)
        }
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

    @FXML private void handlePlay() {
        println("Play")
    }

    @FXML private void handleStop() {
        println("Stop")
    }

    @FXML private void handleRecord() {
        println("Record")
    }
}