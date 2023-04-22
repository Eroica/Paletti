import app.paletti.lib.Leptonica
import javafx.beans.InvalidationListener
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.io.File
import javax.imageio.ImageIO

interface IPosterizedImage {
    val source: String
    val colors: List<Color>
}

class PosterizedPix(
    private val sqlImage: SqlImage,
    cacheDir: File,
) : IPosterizedImage by sqlImage {
    val path: String = cacheDir.resolve("${sqlImage.id}.png").toURI().toString()
}

class ViewModel(
    private val id: Int,
    private val images: SqlImages,
    private val databasePath: String,
    private val cacheDir: File,
    dispatcher: CoroutineDispatcher
) {
    private val context = CoroutineScope(dispatcher + SupervisorJob())

    private val count = SimpleIntegerProperty(6)
    fun getCount() = count.get()
    fun setCount(value: Int) {
        count.set(value)
    }
    fun countProperty() = count

    private val isBlackWhite = SimpleBooleanProperty(false)
    fun getIsBlackWhite() = isBlackWhite.get()
    fun setIsBlackWhite(value: Boolean) {
        isBlackWhite.set(value)
    }
    fun isBlackWhiteProperty() = isBlackWhite

    private val isCropImage = SimpleBooleanProperty(true)
    fun getIsCropImage() = isCropImage.get()
    fun isCropImageProperty() = isCropImage

    private val isRestoreImage = SimpleBooleanProperty(false)
    fun getIsRestoreImage() = isRestoreImage.get()
    fun setIsRestoreImage(value: Boolean) {
        isRestoreImage.set(value)
    }
    fun isRestoreImageProperty() = isRestoreImage

    private val isAlwaysDarkMode = SimpleBooleanProperty(false)
    fun getIsAlwaysDarkMode() = isAlwaysDarkMode.get()
    fun setIsAlwaysDarkMode(value: Boolean) {
        isAlwaysDarkMode.set(value)
    }
    fun isAlwaysDarkModeProperty() = isAlwaysDarkMode

    private val image = SimpleObjectProperty<PosterizedPix?>(null)
    fun getImage() = image.get()
    fun imageProperty(): ReadOnlyObjectProperty<PosterizedPix?> = image

    private var imagePath: String? = null
    private val _posterize = MutableSharedFlow<String?>(0, 1, BufferOverflow.DROP_OLDEST)

    private val onChangeListener = InvalidationListener {
        imagePath?.let { _posterize.tryEmit(it) }
    }

    init {
        count.addListener(onChangeListener)
        isBlackWhite.addListener(onChangeListener)
        _posterize.filterNotNull()
            .debounce(400)
            .onEach { image.set(posterize(it)) }
            .launchIn(context)
    }

    suspend fun load(path: String) = coroutineScope {
        imagePath = path
        image.set(posterize(path))
    }

    suspend fun load(image: Image) = coroutineScope {
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", cacheDir.resolve("tmp.png"))
        load(cacheDir.resolve("tmp.png").toString())
    }

    suspend fun save(destination: File) = coroutineScope {
        cacheDir.resolve("${images[id].id}.png").copyTo(destination, true)
        return@coroutineScope
    }

    fun onCleared() {
        if (!getIsRestoreImage()) {
            images.delete(id)
        }
        context.cancel()
        count.removeListener(onChangeListener)
        isBlackWhite.removeListener(onChangeListener)
    }

    private suspend fun posterize(path: String) = coroutineScope {
        images.delete(id)
        images.add(id, count.get(), isBlackWhite.get(), path)
        images[id].setParameters(count.get(), isBlackWhite.get())
        if (Leptonica.posterize2(id, databasePath) != Leptonica.OK) throw LeptonicaError
        PosterizedPix(images[id], cacheDir)
    }
}
