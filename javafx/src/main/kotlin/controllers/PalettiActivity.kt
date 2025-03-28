package controllers

import Uninitialized
import ViewModel
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.embed.swing.SwingFXUtils
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Rectangle2D
import javafx.geometry.Side
import javafx.scene.SnapshotParameters
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.*
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.javafx.asFlow
import ui.COMBINATION_CLOSE
import ui.COMBINATION_OPEN
import ui.COMBINATION_PASTE_FROM_CLIPBOARD
import views.ColorTile
import views.FluentMenu
import views.Notification
import java.io.IOException
import java.nio.file.Paths
import javax.imageio.ImageIO

interface IWindow {
    fun close()
    fun stage(): Stage
}

interface ISaveDialog {
    fun saveImage()
    fun savePalette()
}

class PalettiActivity(
    val viewModel: ViewModel,
    private val window: IWindow
) : StackPane(), ISaveDialog {
    @FXML
    private lateinit var optionsButton: Button

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
    private lateinit var ditherSwitch: CheckBox

    @FXML
    private lateinit var colorPalette: HBox

    @FXML
    private lateinit var cropImageItem: CheckMenuItem

    @FXML
    private lateinit var restoreImageItem: CheckMenuItem

    @FXML
    private lateinit var alwaysDarkMode: CheckMenuItem

    private val context = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        throwable.message?.let {
            Platform.runLater { notification.show(it) }
        }
    })

    private val optionsMenu = FluentMenu()

    private var isScrollTrackpad = false

    init {
        FXMLLoader(javaClass.getResource("PalettiActivity.fxml")).apply {
            setRoot(this@PalettiActivity)
            setController(this@PalettiActivity)
            load()
        }

        FXMLLoader(javaClass.getResource("/menus/Menu_Activity.fxml")).apply {
            setRoot(optionsMenu)
            setController(this@PalettiActivity)
            load()
        }

        slider.valueProperty().bindBidirectional(viewModel.countProperty())
        viewModel.countProperty().addListener { _, _, count -> setColorPalette(count.toInt()) }
        monoSwitch.selectedProperty().bindBidirectional(viewModel.isBlackWhiteProperty())
        ditherSwitch.selectedProperty().bindBidirectional(viewModel.isDitherProperty())
        cropImageItem.selectedProperty().bindBidirectional(viewModel.isCropImageProperty())
        restoreImageItem.selectedProperty().bindBidirectional(viewModel.isRestoreImageProperty())
        alwaysDarkMode.selectedProperty().bindBidirectional(viewModel.isAlwaysDarkModeProperty())

        /* Adds the initial number of color tiles in the lower palette */
        while (colorPalette.children.size < viewModel.getCount()) {
            colorPalette.children.add(ColorTile())
        }

        viewModel.imageProperty()
            .asFlow()
            .filterNotNull()
            .onEach {
                val colors = it.colors
                withContext(Dispatchers.Main) {
                    setColorPalette(colors.size)
                    colors.forEachIndexed { index, color ->
                        (colorPalette.children[index] as ColorTile).setColor(color)
                    }
                }
            }
            .launchIn(context)

        viewModel.imageProperty()
            .asFlow()
            .filterNotNull()
            .take(1)
            .onEach {
                val image = Image(
                    it.path, fragmentContainer.width, fragmentContainer.height, true, true, true
                )
                image.progressProperty().addListener(object : ChangeListener<Number> {
                    override fun changed(
                        observable: ObservableValue<out Number>?,
                        oldValue: Number?,
                        newValue: Number?
                    ) {
                        if ((newValue?.toDouble() ?: 0.0) >= 1.0) {
                            image.progressProperty().removeListener(this)
                            val fragment = ImageFragment(
                                viewModel,
                                fragmentContainer.width,
                                fragmentContainer.height,
                                image,
                                this@PalettiActivity,
                                notification
                            )
                            fragment.imageView.fitWidthProperty().bind(fragmentContainer.widthProperty())
                            fragment.imageView.fitHeightProperty().bind(fragmentContainer.heightProperty())

                            Platform.runLater {
                                fragmentContainer.children.removeAt(0)
                                fragmentContainer.children.add(0, fragment)
                                this@PalettiActivity.fragment = fragment
                            }
                        }
                    }
                })
            }
            .launchIn(context)
    }

    override fun saveImage() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PNG Image", "*.png"))
        fileChooser.showSaveDialog(window.stage())?.let {
            context.launch { viewModel.save(it) }
        }
    }

    override fun savePalette() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PNG Image", "*.png"))
        fileChooser.showSaveDialog(window.stage())?.let {
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

    fun onDestroy() {
        slider.valueProperty().unbindBidirectional(viewModel.countProperty())
        monoSwitch.selectedProperty().unbindBidirectional(viewModel.isBlackWhiteProperty())
        ditherSwitch.selectedProperty().unbindBidirectional(viewModel.isDitherProperty())
        cropImageItem.selectedProperty().unbindBidirectional(viewModel.isCropImageProperty())
        restoreImageItem.selectedProperty().unbindBidirectional(viewModel.isRestoreImageProperty())
        alwaysDarkMode.selectedProperty().unbindBidirectional(viewModel.isAlwaysDarkModeProperty())
        fragment.onDestroy()
        context.cancel()
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
        val path = event.dragboard.files.first().absolutePath
        context.launch { viewModel.load(path) }
        event.consume()
    }

    fun onScroll(event: ScrollEvent) {
        if (!isScrollTrackpad && !event.isInertia) {
            if (event.deltaY > 0) {
                slider.value++
            } else {
                slider.value--
            }
        }
        event.consume()
    }

    fun onScrollStarted(event: ScrollEvent) {
        isScrollTrackpad = true
        event.consume()
    }

    fun onScrollFinished(event: ScrollEvent) {
        isScrollTrackpad = false
        event.consume()
    }

    fun onAlwaysOnTop(event: ActionEvent) {
        window.stage().let {
            if (it.isAlwaysOnTop) {
                it.isAlwaysOnTop = false
                it.scene.root.styleClass.remove("is-always-on-top")
            } else {
                it.isAlwaysOnTop = true
                it.scene.root.styleClass.add("is-always-on-top")
            }
        }
        event.consume()
    }

    fun onOptionsClick(event: ActionEvent) {
        optionsMenu.show(optionsButton, Side.BOTTOM, 0.0, 0.0)
        event.consume()
    }

    fun onKeyPressed(event: KeyEvent) {
        try {
            when {
                event.code == KeyCode.UP -> {
                    slider.value++
                    event.consume()
                }

                event.code == KeyCode.DOWN -> {
                    slider.value--
                    event.consume()
                }

                event.code == KeyCode.X -> {
                    monoSwitch.isSelected = !monoSwitch.isSelected
                    event.consume()
                }

                event.code == KeyCode.Y -> {
                    ditherSwitch.isSelected = !ditherSwitch.isSelected
                    event.consume()
                }

                COMBINATION_OPEN.match(event) -> {
                    openFileDialog()
                    event.consume()
                }

                COMBINATION_CLOSE.match(event) -> {
                    window.close()
                    event.consume()
                }

                COMBINATION_PASTE_FROM_CLIPBOARD.match(event) -> {
                    val clipboard = Clipboard.getSystemClipboard()

                    if (clipboard.hasImage()) {
                        val image = clipboard.image
                        context.launch { viewModel.load(image) }
                    } else if (clipboard.hasFiles()) {
                        val path = clipboard.files.first().absolutePath
                        context.launch { viewModel.load(path) }
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

    @FXML
    private fun onAboutClick(event: ActionEvent) {
        AboutDialog(window).show()
        event.consume()
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
        fileChooser.showOpenDialog(window.stage())?.let {
            context.launch { viewModel.load(it.absolutePath) }
        }
    }
}
