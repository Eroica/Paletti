package components

import IMessage
import javafx.animation.Interpolator
import javafx.animation.PauseTransition
import javafx.animation.SequentialTransition
import javafx.animation.TranslateTransition
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

    override fun show(message: String) {
        if (!isRunning) {
            isRunning = true
            val label = FXMLLoader.load<Label>(javaClass.getResource("Notification_TypeError.fxml"))
            label.text = message
            label.translateY = height / 12
            children.add(label)

            val slideUp = TranslateTransition(ANIM_DURATION, label)
            slideUp.byY = -height / 12
            slideUp.interpolator = EASING
            val slideDown = TranslateTransition(ANIM_DURATION, label)
            slideDown.byY = height / 6
            slideDown.interpolator = EASING

            SequentialTransition(slideUp, PauseTransition(Duration.seconds(2.0)), slideDown).apply {
                setOnFinished {
                    this@Notification.children.remove(label)
                    isRunning = false
                }
                play()
            }
        }
    }
}
