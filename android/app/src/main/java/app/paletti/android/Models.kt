package app.paletti.android

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths

data class FilePaths(
    val colors: File,
    val copiedImage: File,
    val tmpImage: File,
    val outImage: File,
    val palette: File
)

class ColorPalette(
    private val bitmap: Bitmap,
    private val colors: List<Int>
) {
    fun renderTo(outputFile: File) {
        val canvas = Canvas(bitmap)
        val paint = Paint()
        colors.forEachIndexed { i, color ->
            paint.color = color
            canvas.drawRect(i * 48f, 0f, (i + 1) * 48f, 96f, paint)
        }

        return FileOutputStream(outputFile).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

class PalettiImage(
    val sourceFile: File,
) {
    private val directory = Environment.DIRECTORY_PICTURES + File.separator + "Paletti"

    val filename = System.currentTimeMillis().toString() + ".bmp"

    fun storeWith(resolver: ContentResolver): Path {
        ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/bmp")
            put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, this)?.let {
                resolver.openOutputStream(it)?.use {
                    sourceFile.inputStream().copyTo(it)
                }
            }
        }

        return Paths.get(directory, filename)
    }
}
