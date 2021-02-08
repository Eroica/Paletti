package components

import javafx.beans.DefaultProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.stage.Screen
import javafx.stage.Stage

@DefaultProperty("children")
class HeaderBar : HBox() {
    @FXML
    private lateinit var maximizeButton: Button

    private var originalWidth = 0.0
    private var originalHeight = 0.0
    private var originalLeft = 0.0
    private var originalTop = 0.0
    private var offsetX = 0.0
    private var offsetY = 0.0

    var stage: Stage? = null
    val isMaximized = SimpleBooleanProperty(false)

    init {
        FXMLLoader(javaClass.getResource("HeaderBar.fxml")).apply {
            setRoot(this@HeaderBar)
            setController(this@HeaderBar)
            load()
        }
    }

    override fun getChildren(): ObservableList<Node> {
        return FXCollections.observableList(super.getChildren().asReversed())
    }

    fun onHeaderBarPressed(event: MouseEvent) {
        stage?.let {
            offsetX = it.x - event.screenX
            offsetY = it.y - event.screenY
        }
        event.consume()
    }

    fun onHeaderBarDragged(event: MouseEvent) {
        if (!isMaximized.value) {
            stage?.let {
                it.x = event.screenX + offsetX
                it.y = event.screenY + offsetY
            }
        }
        event.consume()
    }

    fun onClose(event: ActionEvent) {
        stage?.close()
        event.consume()
    }

    fun onMaximize(event: ActionEvent) {
        isMaximized.value = !isMaximized.value
        stage?.let {
            if (isMaximized.value) {
                it.scene.root.styleClass.remove("stage-shadow")
                maximizeButton.text = "\uE923"
                originalWidth = it.width
                originalHeight = it.height
                originalLeft = it.x
                originalTop = it.y
                Screen.getPrimary().visualBounds.apply {
                    it.width = width
                    it.height = height
                    it.x = minX
                    it.y = minY
                }
            } else {
                it.scene.root.styleClass.add("stage-shadow")
                maximizeButton.text = "\uE922"
                it.x = originalLeft
                it.y = originalTop
                it.width = originalWidth
                it.height = originalHeight
            }
        }
        event.consume()
    }

    fun onMinimize(event: ActionEvent) {
        stage?.isIconified = true
        event.consume()
    }

    fun onAlwaysOnTop(event: ActionEvent) {
        stage?.let {
            if (it.isAlwaysOnTop) {
                it.isAlwaysOnTop = false
                it.scene.root.styleClass.remove("is-always-on-top")
            } else {
                it.isAlwaysOnTop = true
                it.scene.root.styleClass.add("is-always-on-top")
            }
        }
    }
}
