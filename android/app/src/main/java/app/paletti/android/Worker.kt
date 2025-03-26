package app.paletti.android

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.paletti.lib.Leptonica
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance
import java.io.File
import java.io.IOException

private fun duplicateToBmp(source: File, destination: File) {
    val bitmap = BitmapFactory.decodeFile(source.toString())
    AndroidBmpUtil.save(bitmap, destination.toString())
    bitmap.recycle()
}

class CopyWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params), DIGlobalAware {
    companion object {
        const val URI = "URI"
    }

    private val Paths: FilePaths by instance()

    override fun doWork(): Result {
        return try {
            applicationContext.contentResolver.openInputStream(Uri.parse(inputData.getString(URI))).use {
                it?.copyTo(Paths.copiedImage.outputStream())
            }
            duplicateToBmp(Paths.copiedImage, Paths.tmpImage)
            Result.success()
        } catch (e: IOException) {
            Result.failure()
        }
    }
}

class PosterizeWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params), DIGlobalAware {
    companion object {
        const val WORK_NAME = "posterize"
        const val COUNT = "COUNT"
        const val IS_BLACK_WHITE = "IS_BLACK_WHITE"
    }

    private val Paths: FilePaths by instance()

    override fun doWork(): Result {
        val count = inputData.getInt(COUNT, 6)
        val isBlackWhite = inputData.getBoolean(IS_BLACK_WHITE, false)
        val ok = Leptonica.posterize(
            count,
            isBlackWhite,
            arrayOf(Paths.tmpImage.toString(), Paths.outImage.toString(), Paths.colors.toString())
        )
        return if (ok == 0) Result.success() else Result.failure()
    }
}
