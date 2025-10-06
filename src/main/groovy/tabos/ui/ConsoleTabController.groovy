package tabos.ui

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Context
import ch.qos.logback.core.OutputStreamAppender
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.TextArea
import javafx.scene.text.Font
import org.slf4j.LoggerFactory

class ConsoleTabController implements Initializable {
    @FXML private TextArea console

    @Override
    void initialize(URL url, ResourceBundle resourceBundle) {
        console.setFont(Font.font('monospace', 13))
        Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.addAppender(new TextAreaAppender(console).tap {
            start()
        })
    }
}

class TextAreaAppender extends OutputStreamAppender<ILoggingEvent> {
    TextAreaAppender(TextArea textArea) {
        Context lc = LoggerFactory.getILoggerFactory() as Context
        setContext(lc)
        encoder = new PatternLayoutEncoder().tap {
            pattern = "%date %-5level [%thread] %logger{20} -- %msg%n"
            setContext lc
            start()
        }
        outputStream = new TextAreaOutputStream(textArea)
    }
}

class TextAreaOutputStream extends OutputStream {
    private final TextArea textArea

    TextAreaOutputStream(TextArea textArea) { this.textArea = textArea }

    @Override void write(int b) throws IOException { super.write(b) }

    @Override
    void write(byte[] bytes) throws IOException {
        Platform.runLater(() -> textArea.appendText(new String(bytes)))
    }
}
