import app.paletti.lib.Leptonica
import javafx.beans.InvalidationListener
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.io.File
import javax.imageio.ImageIO

class ViewModel(
    private val id: Int,
    private val images: SqlImages,
    private val appDir: AppDir,
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

    private val isDither = SimpleBooleanProperty(false)
    fun getIsDither() = isDither.get()
    fun setIsDither(value: Boolean) {
        isDither.set(value)
    }
    fun isDitherProperty() = isDither

    private val isCropImage = SimpleBooleanProperty(true)
    fun getIsCropImage() = isCropImage.get()
    fun isCropImageProperty() = isCropImage

    private val isRestoreImage = object : SimpleBooleanProperty(appDir.database.isRestoreImage) {
        override fun set(newValue: Boolean) {
            super.set(newValue)
            appDir.database.isRestoreImage = newValue
        }
    }

    fun getIsRestoreImage() = isRestoreImage.get()
    fun isRestoreImageProperty() = isRestoreImage

    private val isAlwaysDarkMode = object : SimpleBooleanProperty(appDir.database.isAlwaysDarkMode) {
        override fun set(newValue: Boolean) {
            super.set(newValue)
            appDir.database.isAlwaysDarkMode = newValue
        }
    }

    fun getIsAlwaysDarkMode() = isAlwaysDarkMode.get()
    fun isAlwaysDarkModeProperty() = isAlwaysDarkMode

    private val image = SimpleObjectProperty<PosterizedPix?>(null)
    fun getImage() = image.get()
    fun imageProperty(): ReadOnlyObjectProperty<PosterizedPix?> = image

    private val isImageLoaded = image.isNotNull
    fun getIsImageLoaded() = isImageLoaded.get()
    fun isImageLoadedProperty(): BooleanBinding = isImageLoaded

    private var imagePath: String? = null
    private val _posterize = MutableSharedFlow<String?>(0, 1, BufferOverflow.DROP_OLDEST)

    private val onChangeListener = InvalidationListener {
        imagePath?.let { _posterize.tryEmit(it) }
    }

    init {
        if (appDir.database.isRestoreImage) {
            try {
                setCount(images[id].count)
                setIsBlackWhite(images[id].isBlackWhite)
                runBlocking { load(images[id].source) }
            } catch (e: Exception) {
                setCount(6)
                setIsBlackWhite(false)
                isRestoreImage.set(false)
            }
        }

        count.addListener(onChangeListener)
        isBlackWhite.addListener(onChangeListener)
        isDither.addListener(onChangeListener)
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
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", appDir.resolve("tmp.png"))
        load(appDir.resolve("tmp.png").toString())
    }

    suspend fun save(destination: File) = coroutineScope {
        appDir.resolve("${images[id].id}.png").copyTo(destination, true)
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
        images.add(id, count.get(), isBlackWhite.get(), false, path)
        images[id].setParameters(count.get(), isBlackWhite.get(), isDither.get())
        if (Leptonica.posterize2(id, appDir.databasePath) != Leptonica.OK) throw LeptonicaError
        PosterizedPix(images[id], appDir)
    }
}
