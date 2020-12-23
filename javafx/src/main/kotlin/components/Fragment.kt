package components

import IViewModel
import Uninitialized
import javafx.beans.NamedArg
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox

interface IFragment {
    suspend fun onLoad(path: String)
    suspend fun onLoad(image: Image)
    fun onShortcut(event: KeyEvent)
}

class InitialFragment(@NamedArg("navigation") private val navigation: INavigation) : VBox(), IFragment {
    init {
        FXMLLoader(javaClass.getResource("FragmentInitial.fxml")).apply {
            setRoot(this@InitialFragment)
            setController(this@InitialFragment)
            load()
        }
    }

    override suspend fun onLoad(path: String) {
        navigation.next(path)
    }

    override suspend fun onLoad(image: Image) {
        navigation.next(image)
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
}

class ImageFragment(private val saveDialog: ISaveDialog, private val viewModel: IViewModel) : StackPane(), IFragment {
    @FXML
    lateinit var imageView: ImageView

    init {
        FXMLLoader(javaClass.getResource("FragmentImage.fxml")).apply {
            setRoot(this@ImageFragment)
            setController(this@ImageFragment)
            load()
        }
        viewModel.pix.addListener { _, _, pix ->
            imageView.image = SwingFXUtils.toFXImage(pix?.image, null)
        }
    }

    override suspend fun onLoad(path: String) {
        viewModel.load(path)
    }

    override suspend fun onLoad(image: Image) {
        viewModel.load(image)
    }

    override fun onShortcut(event: KeyEvent) {
        when {
            COMBINATION_SAVE.match(event) -> {
                viewModel.pix.value?.let { saveDialog.saveImage(it.src) }
            }
            COMBINATION_EXPORT_PALETTE.match(event) -> {
                viewModel.pix.value?.let { saveDialog.savePalette() }
            }
            COMBINATION_COPY_TO_CLIPBOARD.match(event) -> {
                Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
                    putImage(imageView.image)
                })
            }
        }
        event.consume()
    }
}
