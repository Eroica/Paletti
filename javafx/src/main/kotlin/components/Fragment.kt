package components

import IViewModel
import Uninitialized
import io.reactivex.disposables.CompositeDisposable
import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Rectangle2D
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.ContextMenu
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox

interface IFragment {
    fun onShortcut(event: KeyEvent)
    fun onDestroy() = Unit
}

class InitialFragment : VBox(), IFragment {
    init {
        FXMLLoader(javaClass.getResource("FragmentInitial.fxml")).apply {
            setRoot(this@InitialFragment)
            setController(this@InitialFragment)
            load()
        }
    }

    override fun onShortcut(event: KeyEvent) {
        when {
            COMBINATION_SAVE.match(event) ||
                COMBINATION_EXPORT_PALETTE.match(event) ||
                COMBINATION_COPY_TO_CLIPBOARD.match(event) -> {
                throw Uninitialized
            }
        }
    }
}

private fun fitRectangle(width: Double, height: Double, outerWidth: Double, outerHeight: Double): Rectangle2D {
    val innerAspect = width / height
    val outerAspect = outerWidth / outerHeight

    var targetWidth = outerWidth
    var targetHeight = outerHeight
    if (outerAspect >= innerAspect) {
        targetWidth = targetHeight * innerAspect
    } else {
        targetHeight = targetWidth / innerAspect
    }
    val minX = (outerWidth - targetWidth) / 2
    val minY = (outerHeight - targetHeight) / 2

    return Rectangle2D(minX, minY, targetWidth, targetHeight)
}

class ImageFragment(
    viewModel: IViewModel,
    containerWidth: Double,
    containerHeight: Double,
    private val saveDialog: ISaveDialog,
    private val notification: INotification
) : StackPane(), IFragment {
    @FXML
    lateinit var imageView: ImageView

    private val cropImageItem = CheckMenuItem("Crop and zoom image").apply { isSelected = true }
    private val disposables = CompositeDisposable()

    init {
        FXMLLoader(javaClass.getResource("FragmentImage.fxml")).apply {
            setRoot(this@ImageFragment)
            setController(this@ImageFragment)
            load()
        }

        val imageContextMenu = ContextMenu().apply { items.add(cropImageItem) }
        imageView.setOnContextMenuRequested {
            imageContextMenu.show(imageView, it.screenX, it.screenY)
        }

        cropImageItem.selectedProperty().addListener(InvalidationListener {
            imageView.image?.let { image ->
                if (!cropImageItem.isSelected) {
                    imageView.viewport = Rectangle2D(0.0, 0.0, image.width, image.height)
                } else {
                    imageView.viewport = fitRectangle(this.width, this.height, image.width, image.height)
                }
            }
        })
        disposables.add(viewModel.image.subscribe {
            val width = if (this.width == 0.0) containerWidth else this.width
            val height = if (this.height == 0.0) containerHeight else this.height
            val image = Image(it.path, width, height, true, true, true)
            image.progressProperty().addListener(object : ChangeListener<Number> {
                override fun changed(observable: ObservableValue<out Number>?, oldValue: Number?, newValue: Number?) {
                    if (newValue?.toDouble() ?: 0.0 >= 1.0) {
                        if (!cropImageItem.isSelected) {
                            imageView.viewport = Rectangle2D(0.0, 0.0, image.width, image.height)
                        } else {
                            imageView.viewport = fitRectangle(width, height, image.width, image.height)
                        }
                        imageView.image = image
                        image.progressProperty().removeListener(this)
                    }
                }
            })
        })

        val resizeListener = InvalidationListener {
            imageView.image?.let { image ->
                if (cropImageItem.isSelected) {
                    imageView.viewport = fitRectangle(this@ImageFragment.width, this@ImageFragment.height, image.width, image.height)
                }
            }
        }
        this.widthProperty().addListener(resizeListener)
        this.heightProperty().addListener(resizeListener)
    }

    override fun onShortcut(event: KeyEvent) {
        when {
            COMBINATION_SAVE.match(event) -> {
                saveDialog.saveImage()
                event.consume()
            }
            COMBINATION_EXPORT_PALETTE.match(event) -> {
                saveDialog.savePalette()
                event.consume()
            }
            COMBINATION_COPY_TO_CLIPBOARD.match(event) -> {
                Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
                    putImage(imageView.image)
                })
                notification.show("Copied current image to clipboard.")
                event.consume()
            }
        }
    }

    override fun onDestroy() {
        disposables.dispose()
    }
}
