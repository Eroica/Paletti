import app.paletti.lib.FilePaths
import app.paletti.lib.Leptonica
import javafx.scene.image.Image
import javafx.scene.paint.Color

interface IPosterizedImage {
    val path: String
    val image: Image
    val colors: ArrayList<Color>
}

class PosterizedImage(
    source: String,
    count: Int,
    isBlackWhite: Boolean,
    private val filePaths: FilePaths
) : IPosterizedImage {
    override val path = source
    override val image: Image by lazy { Image(filePaths.outImage.inputStream()) }
    override val colors: ArrayList<Color>
        get() {
            val colors = ArrayList<Color>()
            filePaths.colors.forEachLine {
                val rgb = it.split(",").map { it.toInt() }
                colors.add(Color.rgb(rgb[0], rgb[1], rgb[2]))
            }
            return colors
        }

    init {
        val ok = Leptonica.posterize(
            count, isBlackWhite, arrayOf(
                source,
                filePaths.outImage.toString(),
                filePaths.colors.toString()
            )
        )
        if (ok != 0) {
            throw LeptonicaError
        }
    }
}
