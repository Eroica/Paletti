package components

import IMessage
import javafx.animation.*
import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.util.Duration

private val ANIM_DURATION = Duration.millis(800.0)
private val EASING = Interpolator.SPLINE(.02, .98, .46, .95)

interface INotification {
    fun show(message: IMessage)
    fun show(message: String)
}

class Notification : StackPane(), INotification {
    private var isRunning = false

    init {
        FXMLLoader(javaClass.getResource("NotificationPane.fxml")).apply {
            setRoot(this@Notification)
            setController(this@Notification)
            load()
        }
    }

    override fun show(message: IMessage) {
        show(message.toString())
    }

    private val slideUp = TranslateTransition(ANIM_DURATION).apply {
        toY = 0.0
        interpolator = EASING
    }

    private val slideDown = TranslateTransition(ANIM_DURATION).apply {
        interpolator = EASING
    }

    private val pause = PauseTransition(Duration.seconds(2.0))

    override fun show(message: String) {
        if (!isRunning) {
            isRunning = true
            val label = FXMLLoader.load<Label>(javaClass.getResource("Notification_TypeError.fxml"))
            label.text = message
            label.translateYProperty().bind(label.heightProperty().add(4.0))

            children.add(label)
            applyCss()
            layout()
            label.translateYProperty().unbind()
            slideUp.node = label
            slideDown.node = label
            slideDown.toY = label.height + 4.0

            SequentialTransition(slideUp, pause, slideDown).apply {
                setOnFinished {
                    this@Notification.children.remove(label)
                    slideUp.node = null
                    slideDown.node = null
                    isRunning = false
                }
                play()
            }
        }
    }
}
