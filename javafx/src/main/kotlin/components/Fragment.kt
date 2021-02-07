package components

import IViewModel
import Uninitialized
import io.reactivex.disposables.CompositeDisposable
import javafx.beans.InvalidationListener
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Rectangle2D
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

class ImageFragment(
    viewModel: IViewModel,
    private val saveDialog: ISaveDialog,
    viewportRectangle: ObservableValue<Rectangle2D>
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
        viewportRectangle.addListener(InvalidationListener {
            val r = viewportRectangle.value
            val image = imageView.image
            val minX = (image.width - r.width) / 2
            val minY = (image.height - r.height) / 2
            imageView.viewport = Rectangle2D(minX, minY, r.width, r.height)
        })
        imageView.viewport = viewportRectangle.value
        disposables.add(viewModel.image.subscribe {
            val image = it.image
            val r = viewportRectangle.value
            val minX = (image.width - r.width) / 2
            val minY = (image.height - r.height) / 2
            imageView.image = image
            imageView.viewport = Rectangle2D(minX, minY, r.width, r.height)
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
