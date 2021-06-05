package components

import IViewModel
import Uninitialized
import io.reactivex.disposables.CompositeDisposable
import javafx.beans.InvalidationListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Rectangle2D
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox

interface IFragment {
    fun onShortcut(event: KeyEvent)
    fun onDestroy()
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
        event.consume()
    }

    override fun onDestroy() {}
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
    private val saveDialog: ISaveDialog,
) : StackPane(), IFragment {
    @FXML
    lateinit var imageView: ImageView

    private val disposables = CompositeDisposable()

    init {
        FXMLLoader(javaClass.getResource("FragmentImage.fxml")).apply {
            setRoot(this@ImageFragment)
            setController(this@ImageFragment)
            load()
        }

        val resizeListener = InvalidationListener {
            imageView.image?.let { image ->
                imageView.viewport = fitRectangle(this@ImageFragment.width, this@ImageFragment.height, image.width, image.height)
            }
        }
        this.widthProperty().addListener(resizeListener)
        this.heightProperty().addListener(resizeListener)

        disposables.add(viewModel.image.subscribe {
            imageView.viewport = fitRectangle(this.width, this.height, it.image.width, it.image.height)
            imageView.image = it.image
        })
    }

    override fun onShortcut(event: KeyEvent) {
        when {
            COMBINATION_SAVE.match(event) -> saveDialog.saveImage()
            COMBINATION_EXPORT_PALETTE.match(event) -> saveDialog.savePalette()
            COMBINATION_COPY_TO_CLIPBOARD.match(event) -> {
                Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
                    putImage(imageView.image)
                })
            }
        }
        event.consume()
    }

    override fun onDestroy() {
        disposables.dispose()
    }
}
