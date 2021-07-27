package views

import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Skin
import javafx.scene.control.Slider
import javafx.scene.control.skin.SliderSkin
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.util.Duration

class FluentSlider : Slider() {
    override fun createDefaultSkin(): Skin<*> {
        return FluentSliderSkin(this)
    }
}

class FluentSliderSkin(slider: FluentSlider) : SliderSkin(slider) {
    private val thumb = skinnable.lookup(".thumb") as StackPane

    init {
        val radiusProperty = SimpleIntegerProperty()
        this.thumb.styleProperty().bind(SimpleStringProperty("-fx-background-insets: -8px, -6px, -5px, ")
            .concat(radiusProperty)
            .concat("px;"))
        val scaleMoveIn = Timeline(KeyFrame(Duration.millis(0.0), KeyValue(radiusProperty, 0)),
            KeyFrame(Duration.millis(100.0), KeyValue(radiusProperty, -2, Interpolator.EASE_IN)))
        val scaleMoveOut = Timeline(KeyFrame(Duration.millis(0.0), KeyValue(radiusProperty, -2)),
            KeyFrame(Duration.millis(100.0), KeyValue(radiusProperty, 0, Interpolator.EASE_OUT)))
        val scalePressed = Timeline(KeyFrame(Duration.millis(0.0), KeyValue(radiusProperty, 0)),
            KeyFrame(Duration.millis(60.0), KeyValue(radiusProperty, 1, Interpolator.EASE_IN)))
        val scaleReleased = Timeline(KeyFrame(Duration.millis(0.0), KeyValue(radiusProperty, 0)),
            KeyFrame(Duration.millis(60.0), KeyValue(radiusProperty, -2, Interpolator.EASE_IN)))
        this.thumb.addEventFilter(MouseEvent.MOUSE_ENTERED) {
            scaleMoveIn.play()
        }
        this.thumb.addEventFilter(MouseEvent.MOUSE_EXITED) {
            scaleMoveOut.play()
        }
        this.thumb.addEventFilter(MouseEvent.MOUSE_PRESSED) {
            scalePressed.play()
        }
        this.thumb.addEventFilter(MouseEvent.MOUSE_RELEASED) {
            scaleReleased.play()
        }
    }
}
