package components

import IViewModel
import Uninitialized
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Rectangle2D
import javafx.scene.SnapshotParameters
import javafx.scene.control.CheckBox
import javafx.scene.control.Slider
import javafx.scene.image.Image
import javafx.scene.input.*
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.io.IOException
import java.nio.file.Paths
import javax.imageio.ImageIO

interface ISaveDialog {
    fun saveImage()
    fun savePalette()
}

val COMBINATION_OPEN = KeyCodeCombination(KeyCode.O, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_CLOSE = KeyCodeCombination(KeyCode.W, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_SAVE = KeyCodeCombination(KeyCode.S, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_EXPORT_PALETTE = KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_COPY_TO_CLIPBOARD = KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_PASTE_FROM_CLIPBOARD = KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN)

class PalettiWindow(private val viewModel: IViewModel) : Stage(), ISaveDialog {
    @FXML
    private lateinit var headerBar: HeaderBar

    @FXML
    private lateinit var fragmentContainer: StackPane

    @FXML
    private lateinit var fragment: IFragment

    @FXML
    private lateinit var notification: Notification

    @FXML
    private lateinit var slider: Slider

    @FXML
    private lateinit var monoSwitch: CheckBox

    @FXML
    private lateinit var colorPalette: HBox

    private var left = 0.0
    private var top = 0.0

    private val disposables = CompositeDisposable()

    var closeRequest: () -> Unit = {}

    init {
        FXMLLoader(javaClass.getResource("PalettiWindow.fxml")).apply {
            setRoot(this@PalettiWindow)
            setController(this@PalettiWindow)
            load()
        }
        initStyle(StageStyle.TRANSPARENT)
        headerBar.stage = this
        slider.valueProperty().bindBidirectional(viewModel.count)
        slider.applyCss()
        viewModel.count.addListener { _, _, count ->
            slider.lookup(".track").style = "-fx-background-color: linear-gradient(to right, #005A9E ${count.toDouble()/32}, #868686 ${count.toDouble()/32});"
            setColorPalette(count.toInt())
        }
        monoSwitch.selectedProperty().bindBidirectional(viewModel.isBlackWhite)
        viewModel.notification.addListener { _, _, message -> notification.show(message) }
        while (colorPalette.children.size < viewModel.count.value) {
            colorPalette.children.add(ColorTile())
        }
        disposables.add(viewModel.image.observeOn(JavaFxScheduler.platform()).subscribe {
            val colors = it.colors
            setColorPalette(colors.size)
            colors.forEachIndexed { index, color ->
                (colorPalette.children[index] as ColorTile).setColor(color)
            }
        })
        disposables.add(viewModel.image.take(1).subscribe {
            val image = Image(it.path, fragmentContainer.width, fragmentContainer.height, true, true, true)
            image.progressProperty().addListener(object : ChangeListener<Number> {
                override fun changed(observable: ObservableValue<out Number>?, oldValue: Number?, newValue: Number?) {
                    if (newValue?.toDouble() ?: 0.0 >= 1.0) {
                        image.progressProperty().removeListener(this)
                        val fragment = ImageFragment(viewModel, fragmentContainer.width, fragmentContainer.height, image,this@PalettiWindow, notification)
                        fragment.imageView.fitWidthProperty().bind(fragmentContainer.widthProperty())
                        fragment.imageView.fitHeightProperty().bind(fragmentContainer.heightProperty())
                        fragmentContainer.children.removeAt(0)
                        fragmentContainer.children.add(0, fragment)
                        this@PalettiWindow.fragment = fragment
                    }
                }
            })
        })
    }

    override fun saveImage() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PNG Image", "*.png"))
        fileChooser.showSaveDialog(this)?.let { viewModel.save(it) }
    }

    override fun savePalette() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PNG Image", "*.png"))
        fileChooser.showSaveDialog(this)?.let {
            try {
                val viewport = Rectangle2D(0.0, 2.0, colorPalette.width, colorPalette.height - 2.0)
                ImageIO.write(
                    SwingFXUtils.fromFXImage(colorPalette.snapshot(SnapshotParameters().apply {
                        this.viewport = viewport
                    }, null), null),
                    "png", it
                )
                notification.show("Saved palette to ${Paths.get(it.toURI())}")
            } catch (e: IOException) {
                e.message?.let { error -> notification.show(error) } ?: e.printStackTrace()
            }
        }
    }

    override fun close() {
        closeRequest()
        super.close()
    }

    fun onDestroy() {
        slider.valueProperty().unbindBidirectional(viewModel.count)
        monoSwitch.selectedProperty().unbindBidirectional(viewModel.isBlackWhite)
        fragment.onDestroy()
        disposables.dispose()
    }

    fun onResizeBegin(event: MouseEvent) {
        left = event.screenX
        top = event.screenY
        event.consume()
    }

    fun onResize(event: MouseEvent) {
        if (!headerBar.isMaximized.value) {
            width = (width + (event.screenX - left)).coerceAtLeast(minWidth)
            height = (height + (event.screenY - top)).coerceAtLeast(minHeight)
            left = event.screenX
            top = event.screenY
        }
        event.consume()
    }

    fun onDropareaClick(event: MouseEvent) {
        if (!event.isPopupTrigger) {
            openFileDialog()
            event.consume()
        }
    }

    fun onDragOver(event: DragEvent) {
        if (event.dragboard.hasFiles()) {
            event.acceptTransferModes(*TransferMode.ANY)
        }
        event.consume()
    }

    fun onDragDropped(event: DragEvent) {
        viewModel.load(event.dragboard.files.first().absolutePath)
        event.consume()
    }

    fun onScroll(event: ScrollEvent) {
        if (event.deltaY > 0) {
            slider.value++
        } else {
            slider.value--
        }
    }

    fun onKeyPressed(event: KeyEvent) {
        try {
            when {
                event.code == KeyCode.X -> {
                    monoSwitch.isSelected = !monoSwitch.isSelected
                    event.consume()
                }
                COMBINATION_OPEN.match(event) -> {
                    openFileDialog()
                    event.consume()
                }
                COMBINATION_CLOSE.match(event) -> {
                    close()
                    event.consume()
                }
                COMBINATION_PASTE_FROM_CLIPBOARD.match(event) -> {
                    val clipboard = Clipboard.getSystemClipboard()
                    if (clipboard.hasImage()) {
                        viewModel.load(clipboard.image)
                    } else if (clipboard.hasFiles()) {
                        viewModel.load(clipboard.files.first().absolutePath)
                    }
                    event.consume()
                }
                else -> fragment.onShortcut(event)
            }
        } catch (e: Uninitialized) {
            notification.show(e)
            event.consume()
        }
    }

    private fun setColorPalette(count: Int) {
        val currentCount = colorPalette.children.size
        if (currentCount > count) {
            colorPalette.children.remove(count, currentCount)
        } else {
            while (colorPalette.children.size < count) {
                colorPalette.children.add(ColorTile())
            }
        }
    }

    private fun openFileDialog() {
        val fileChooser = FileChooser()
        fileChooser.title = "Select an image"
        fileChooser.showOpenDialog(this)?.let {
            viewModel.load(it.absolutePath)
        }
    }
}
