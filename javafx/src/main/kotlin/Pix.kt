import javafx.scene.paint.Color
import net.sourceforge.lept4j.Leptonica1.*
import net.sourceforge.lept4j.Pix
import net.sourceforge.lept4j.util.LeptUtils
import java.awt.image.BufferedImage
import java.nio.IntBuffer

interface IPix {
    val src: Pix
    val colors: Array<Color>
        get() {
            val r = IntBuffer.allocate(1)
            val g = IntBuffer.allocate(1)
            val b = IntBuffer.allocate(1)
            val a = IntBuffer.allocate(1)
            return Array(src.colormap.n) {
                pixcmapGetRGBA(src.colormap, it, r, g, b, a)
                Color.rgb(r[0], g[0], b[0])
            }
        }
    val image: BufferedImage
        get() = LeptUtils.convertPixToImage(src)
}

class PosterizedPix(src: Pix, colorsCount: Int) : IPix {
    override val src: Pix = pixMedianCutQuantGeneral(
        if (src.colormap != null) { pixConvertTo32(src) } else src, 0,
        8, colorsCount,
        0, 0, 0
    ) ?: throw LeptonicaError
}

class BlackWhitePix(src: Pix) : IPix {
    override val src: Pix = pixAddMinimalGrayColormap8(
        pixRemoveColormap(src, 1)
    ) ?: throw LeptonicaError
}
