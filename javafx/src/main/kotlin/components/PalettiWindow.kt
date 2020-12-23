package components

import IViewModel
import PalettiError
import Uninitialized
import javafx.beans.InvalidationListener
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.lept4j.Pix
import net.sourceforge.lept4j.util.LeptUtils
import java.io.IOException
import java.nio.file.Paths
import javax.imageio.ImageIO

interface INavigation {
    suspend fun next(path: String)
    suspend fun next(image: Image)
}

interface ISaveDialog {
    fun saveImage(pix: Pix)
    fun savePalette()
}

val COMBINATION_OPEN = KeyCodeCombination(KeyCode.O, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_CLOSE = KeyCodeCombination(KeyCode.W, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_SAVE = KeyCodeCombination(KeyCode.S, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_EXPORT_PALETTE = KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_COPY_TO_CLIPBOARD = KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN)
val COMBINATION_PASTE_FROM_CLIPBOARD = KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN)

class PalettiWindow(private val viewModel: IViewModel) : Stage(), INavigation, ISaveDialog, CoroutineScope {
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

    override val coroutineContext = Dispatchers.JavaFx
    private var left = 0.0
    private var top = 0.0

    init {
        FXMLLoader(javaClass.getResource("PalettiWindow.fxml")).apply {
            setRoot(this@PalettiWindow)
            setController(this@PalettiWindow)
            load()
        }
        initStyle(StageStyle.TRANSPARENT)
        headerBar.stage = this
        slider.valueProperty().bindBidirectional(viewModel.colorsCount)
        monoSwitch.selectedProperty().bindBidirectional(viewModel.isBlackWhite)
        viewModel.colorsCount.addListener { _, _, count -> setColorPalette(count.toInt()) }
        viewModel.colors.addListener(InvalidationListener {
            setColorPalette(viewModel.colors.value.size)
            viewModel.colors.value.forEachIndexed { index, color ->
                (colorPalette.children[index] as ColorTile).setColor(color)
            }
        })
        while (colorPalette.children.size < viewModel.colorsCount.value) {
            colorPalette.children.add(ColorTile())
        }
    }

    fun onResizeBegin(event: MouseEvent) {
        left = event.screenX
        top = event.screenY
        event.consume()
    }

    fun onResize(event: MouseEvent) {
        if (!headerBar.isMaximized.value) {
            if (width + event.screenX - left >= minWidth) {
                width = width + event.screenX - left
            } else {
                event.consume()
                return
            }
            if (height + event.screenY - top >= minHeight) {
                height = height + event.screenY - top
            } else {
                event.consume()
                return
            }
            left = event.screenX
            top = event.screenY
        }
        event.consume()
    }

    fun onDropareaClick(event: MouseEvent) {
        openFileDialog()
        event.consume()
    }

    fun onDragOver(event: DragEvent) {
        if (event.dragboard.hasFiles()) {
            event.acceptTransferModes(*TransferMode.ANY)
        }
        event.consume()
    }

    fun onDragDropped(event: DragEvent) {
        val droppedFile = event.dragboard.files.first().absolutePath
        launch {
            try {
                fragment.onLoad(droppedFile)
            } catch (e: PalettiError) {
                notification.show(e)
            }
        }
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
                event.code == KeyCode.X -> monoSwitch.isSelected = !monoSwitch.isSelected
                COMBINATION_OPEN.match(event) -> openFileDialog()
                COMBINATION_CLOSE.match(event) -> close()
                COMBINATION_PASTE_FROM_CLIPBOARD.match(event) -> {
                    val clipboard = Clipboard.getSystemClipboard()
                    if (clipboard.hasImage()) {
                        launch { fragment.onLoad(clipboard.image) }
                    } else if (clipboard.hasFiles()) {
                        launch { fragment.onLoad(clipboard.files.first().absolutePath) }
                    }
                }
                else -> {
                    fragment.onShortcut(event)
                }
            }
            event.consume()
        } catch (e: Uninitialized) {
            notification.show(e)
            event.consume()
        }
    }

    override suspend fun next(path: String) {
        val fragment = ImageFragment(this, viewModel)
        try {
            fragment.onLoad(path)
            setup(fragment)
        } catch (e: PalettiError) {
            notification.show(e)
        }
    }

    override suspend fun next(image: Image) {
        val fragment = ImageFragment(this, viewModel)
        try {
            fragment.onLoad(image)
            setup(fragment)
        } catch (e: PalettiError) {
            notification.show(e)
        }
    }

    override fun saveImage(pix: Pix) {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PNG Image", "*.png"))
        fileChooser.showSaveDialog(this)?.let {
            try {
                ImageIO.write(LeptUtils.convertPixToImage(pix), "png", it)
                notification.show("Saved image to ${Paths.get(it.toURI())}")
            } catch (e: IOException) {
                e.message?.let { error -> notification.show(error) } ?: e.printStackTrace()
            }
        }
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

    private fun setup(imageFragment: ImageFragment) {
        imageFragment.imageView.fitWidthProperty().bind(fragmentContainer.widthProperty())
        imageFragment.imageView.fitHeightProperty().bind(fragmentContainer.heightProperty())
        fragmentContainer.children.removeAt(0)
        fragmentContainer.children.add(0, imageFragment)
        fragment = imageFragment
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
            launch {
                try {
                    fragment.onLoad(it.absolutePath)
                } catch (e: PalettiError) {
                    notification.show(e)
                }
            }
        }
    }
}
