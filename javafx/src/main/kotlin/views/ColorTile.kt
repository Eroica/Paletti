package views

import javafx.animation.ScaleTransition
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.Tooltip
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.Duration
import ui.EASING

class ColorTile(private var color: String? = null) : VBox() {
    init {
        FXMLLoader(javaClass.getResource("ColorTile.fxml")).apply {
            setRoot(this@ColorTile)
            setController(this@ColorTile)
            load()
        }
    }

    fun setColor(color: Color) {
        this.color = "#%02x%02x%02x".format(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        this.style += "-fx-background-color: ${this.color}; -fx-cursor: hand;"
        this.styleClass.add("has-color")
        Tooltip.install(this, Tooltip(this.color))
    }

    @FXML
    private fun onPress(event: MouseEvent) {
        if (color != null) {
            ScaleTransition(Duration.seconds(0.1), this).apply {
                toX = 0.9
                toY = 0.9
                interpolator = EASING
                play()
            }
        }
        event.consume()
    }

    @FXML
    private fun onRelease(event: MouseEvent) {
        if (color != null) {
            ScaleTransition(Duration.seconds(0.1), this).apply {
                toX = 1.0
                toY = 1.0
                interpolator = EASING
                play()
            }
        }
        event.consume()
    }

    @FXML
    fun onClick(event: MouseEvent) {
        if (color != null) {
            Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
                putString(color)
            })
        }
        event.consume()
    }
}
