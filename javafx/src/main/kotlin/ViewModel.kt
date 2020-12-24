import app.paletti.lib.FilePaths
import javafx.beans.property.*
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext

interface IViewModel {
    val count: IntegerProperty
    val isBlackWhite: BooleanProperty
    val image: SimpleObjectProperty<IPosterizedImage>

    suspend fun load(path: String)
    suspend fun load(image: Image)
}

class ImageViewModel(private val filePaths: FilePaths) : IViewModel, CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    override val count: IntegerProperty = SimpleIntegerProperty(6)
    override val isBlackWhite: BooleanProperty = SimpleBooleanProperty(false)
    override val image = SimpleObjectProperty<IPosterizedImage>()

    private var time: Long = 0

    init {
        this.count.addListener { _, _, count ->
            image.value?.let {
                time = System.nanoTime()
                launch {
                    delay(100)
                    val delta = System.nanoTime()
                    // Difference in NANOseconds
                    if (delta - time >= 100000000) {
                        posterize(it.path, count.toInt(), isBlackWhite.value)
                    }
                }
            }

        }
        this.isBlackWhite.addListener { _, _, isBlackWhite ->
            image.value?.let {
                launch {
                    posterize(it.path, count.value, isBlackWhite)
                }
            }
        }
    }

    override suspend fun load(path: String) {
        posterize(path, count.value, isBlackWhite.value)
    }

    override suspend fun load(image: Image) {
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", filePaths.tmpImage)
        posterize(filePaths.tmpImage.toString(), count.value, isBlackWhite.value)
    }

    private suspend fun posterize(path: String, count: Int, isBlackWhite: Boolean) {
        val posterizedImage = coroutineScope { async { PosterizedImage(path, count, isBlackWhite, filePaths) } }
        withContext(Dispatchers.JavaFx) {
            image.value = posterizedImage.await()
        }
    }
}
