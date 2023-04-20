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
    private val track = skinnable.lookup(".track") as StackPane

    init {
        val radiusProperty = SimpleIntegerProperty(6)
        thumb.styleProperty().bind(
            SimpleStringProperty("-fx-background-insets: -2, 0, 1,")
                .concat(radiusProperty)
        )

        track.styleProperty().bind(
            SimpleStringProperty("-fx-background-color: linear-gradient(to right, #0e6aba ")
                .concat(skinnable.valueProperty().divide(skinnable.max))
                .concat(", #868686 ")
                .concat(skinnable.valueProperty().divide(skinnable.max))
                .concat(");")
        )

        val scaleMoveIn = Timeline(
            KeyFrame(Duration.millis(0.0), KeyValue(radiusProperty, 6)),
            KeyFrame(Duration.millis(100.0), KeyValue(radiusProperty, 4, Interpolator.EASE_IN))
        )
        val scaleMoveOut = Timeline(
            KeyFrame(Duration.millis(0.0), KeyValue(radiusProperty, 4)),
            KeyFrame(Duration.millis(100.0), KeyValue(radiusProperty, 6, Interpolator.EASE_OUT))
        )
        val scalePressed = Timeline(
            KeyFrame(Duration.millis(0.0), KeyValue(radiusProperty, 4)),
            KeyFrame(Duration.millis(60.0), KeyValue(radiusProperty, 7, Interpolator.EASE_IN))
        )
        val scaleReleased = Timeline(
            KeyFrame(Duration.millis(0.0), KeyValue(radiusProperty, 7)),
            KeyFrame(Duration.millis(60.0), KeyValue(radiusProperty, 4, Interpolator.EASE_IN))
        )
        thumb.addEventFilter(MouseEvent.MOUSE_ENTERED) { scaleMoveIn.play() }
        thumb.addEventFilter(MouseEvent.MOUSE_EXITED) { scaleMoveOut.play() }
        thumb.addEventFilter(MouseEvent.MOUSE_PRESSED) { scalePressed.play() }
        thumb.addEventFilter(MouseEvent.MOUSE_RELEASED) { scaleReleased.play() }
    }
}
