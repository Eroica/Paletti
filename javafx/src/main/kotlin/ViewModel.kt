import javafx.beans.property.*
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.sourceforge.lept4j.Leptonica1
import net.sourceforge.lept4j.Pix
import net.sourceforge.lept4j.util.LeptUtils
import kotlin.coroutines.CoroutineContext

interface IViewModel {
    val colorsCount: IntegerProperty
    val isBlackWhite: BooleanProperty
    val colors: SimpleObjectProperty<Array<Color>>
    var pix: SimpleObjectProperty<IPix?>

    suspend fun load(path: String)
    suspend fun load(image: Image)
}

class ImageViewModel : IViewModel, CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    override val colorsCount: IntegerProperty = SimpleIntegerProperty(6)
    override val isBlackWhite: BooleanProperty = SimpleBooleanProperty(false)
    override val colors: SimpleObjectProperty<Array<Color>> = SimpleObjectProperty()
    override var pix: SimpleObjectProperty<IPix?> = SimpleObjectProperty(null)
    private var pixSource: Pix? = null

    private var time: Long = 0

    init {
        this.colorsCount.addListener { _, _, count ->
            if (pixSource != null) {
                time = System.nanoTime()
                launch {
                    delay(100)
                    val delta = System.nanoTime()
                    // Difference in NANOseconds
                    if (delta - time >= 100000000) {
                        posterize(count.toInt(), isBlackWhite.value)
                    }
                }
            }
        }
        this.isBlackWhite.addListener { _, _, isBlackWhite ->
            if (pixSource != null) {
                launch { posterize(colorsCount.value, isBlackWhite) }
            }
        }
    }

    override suspend fun load(path: String) {
        pixSource = Leptonica1.pixRead(path) ?: throw LeptonicaReadError
        posterize(colorsCount.value, isBlackWhite.value)
    }

    override suspend fun load(image: Image) {
        pixSource = LeptUtils.convertImageToPix(SwingFXUtils.fromFXImage(image, null))
        posterize(colorsCount.value, isBlackWhite.value)
    }

    private fun posterize(count: Int, isBlackWhite: Boolean) {
        pix.value = if (isBlackWhite) {
            BlackWhitePix(PosterizedPix(pixSource ?: return, count).src)
        } else {
            PosterizedPix(pixSource ?: return, count)
        }
        colors.value = pix.value?.colors
    }
}
