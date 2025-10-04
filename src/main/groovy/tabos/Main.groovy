package tabos

import javafx.application.Application
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Stage

class Main extends Application {
    static void main(String[] args) {
        launch(Main, args)
    }

    @Override
    void start(Stage stage) {
        stage.title = "TabOS"
        stage.scene = new Scene(
            new BorderPane().tap {
                center = new SplitPane(
                    new SplitPane(
                        createSidebar(),
                        createEditor()
                    ).tap {
                        orientation = Orientation.HORIZONTAL
                        dividerPositions = 0.2
                    },
                    createBottomPane()
                ).tap {
                    orientation = Orientation.VERTICAL
                    dividerPositions = 0.75
                }
                top = createToolbar()
            },
            1200,
            800
        )
        stage.show()
    }

    private ToolBar createToolbar() {
        new ToolBar(
            new MenuBar(
                new Menu("File", null,
                    new MenuItem("New"),
                    new MenuItem("Open"),
                    new MenuItem("Save"),
                    new SeparatorMenuItem(),
                    new MenuItem("Exit")
                ),
                new Menu("Edit", null,
                    new MenuItem("Undo"),
                    new MenuItem("Redo"),
                    new SeparatorMenuItem(),
                    new MenuItem("Cut"),
                    new MenuItem("Copy"),
                    new MenuItem("Paste")
                )
            ).tap {
                useSystemMenuBarProperty().set(true)
            },
            new Button("Play"),
            new Button("Stop"),
            new Button("Record"),
            new Separator(),
        )
    }

    private TitledPane createSidebar() {
        new TitledPane("Explorer",
            new TabPane(
                new Tab("Project",
                    new TreeView<>(
                        new TreeItem<>("Project Root").tap {
                            children.addAll(
                                new TreeItem<>("Songs"),
                                new TreeItem<>("Tracks"),
                                new TreeItem<>("Instruments")
                            )
                            expanded = true
                        }
                    )
                ),
                new Tab("Properties",
                    new VBox(10,
                        new Label("Tempo: 120"),
                        new Label("Time Signature: 4/4"),
                        new Label("Key: C Major")
                    ).tap {
                        style = "-fx-padding: 10;"
                    }
                ),
                new Tab("Instruments",
                    new ListView().tap {
                        items.addAll(
                            "Guitar",
                            "Bass",
                            "Drums",
                            "Piano"
                        )
                    }
                )
            ).tap {
                tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            }
        ).tap {
            collapsible = false
            prefWidth = 250
            minWidth = 200
        }
    }

    private TitledPane createBottomPane() {
        TabPane tabPane = new TabPane()
        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        Tab consoleTab = new Tab("Console")
        TextArea console = new TextArea()
        console.editable = false
        console.text = "Console output...\n"
        consoleTab.content = console

        Tab timelineTab = new Tab("Timeline")
        ScrollPane timelineScroll = new ScrollPane()
        HBox timeline = new HBox(5)
        timeline.style = "-fx-padding: 10;"
        (0..50).each { i ->
            Label measure = new Label("|$i")
            timeline.children.add(measure)
        }
        timelineScroll.content = timeline
        timelineTab.content = timelineScroll

        Tab mixerTab = new Tab("Mixer")
        HBox mixer = new HBox(10)
        mixer.style = "-fx-padding: 10;"
        ["Track 1", "Track 2", "Track 3", "Master"].each { track ->
            VBox channel = new VBox(5)
            channel.children.addAll(
                new Label(track),
                new Slider(0, 100, 75).tap { it.orientation = Orientation.VERTICAL; it.prefHeight = 150 },
                new Label("75%")
            )
            mixer.children.add(channel)
        }
        mixerTab.content = mixer

        tabPane.tabs.addAll(consoleTab, timelineTab, mixerTab)

        TitledPane bottomPane = new TitledPane("Output", tabPane)
        bottomPane.collapsible = true
        bottomPane.prefHeight = 200
        bottomPane.minHeight = 100

        return bottomPane
    }

    private StackPane createEditor() {
        new StackPane(
            new ScrollPane(
                new VBox(20, *(1..5).collect { track ->
                    new VBox(2,
                        new Label("Track $track"),
                        *(0..4).collect {
                            new Region().tap {
                                prefHeight = 1
                                style = "-fx-background-color: black;"
                            }
                        }
                    )
                }).tap {
                    style = "-fx-padding: 20; -fx-background-color: white;"
                }
            ).tap {
                fitToWidth = true
            }
        )
    }
}
