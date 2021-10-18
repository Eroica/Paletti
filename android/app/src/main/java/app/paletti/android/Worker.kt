package app.paletti.android

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.paletti.lib.Leptonica
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException

private fun duplicateToBmp(source: File, destination: File) {
    val bitmap = BitmapFactory.decodeFile(source.toString())
    AndroidBmpUtil.save(bitmap, destination.toString())
    bitmap.recycle()
}

@HiltWorker
class CopyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    val filePaths: FilePaths
) : Worker(context, params) {
    companion object {
        const val URI = "URI"
    }

    override fun doWork(): Result {
        return try {
            applicationContext.contentResolver.openInputStream(Uri.parse(inputData.getString(URI))).use {
                it?.copyTo(filePaths.copiedImage.outputStream())
            }
            duplicateToBmp(filePaths.copiedImage, filePaths.tmpImage)
            Result.success()
        } catch (e: IOException) {
            Result.failure()
        }
    }
}

@HiltWorker
class PosterizeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    val filePaths: FilePaths
) : Worker(context, params) {
    companion object {
        const val WORK_NAME = "posterize"
        const val COUNT = "COUNT"
        const val IS_BLACK_WHITE = "IS_BLACK_WHITE"
    }

    override fun doWork(): Result {
        val count = inputData.getInt(COUNT, 6)
        val isBlackWhite = inputData.getBoolean(IS_BLACK_WHITE, false)
        val ok = Leptonica.posterize(
            count,
            isBlackWhite,
            arrayOf(filePaths.tmpImage.toString(), filePaths.outImage.toString(), filePaths.colors.toString())
        )
        return if (ok == 0) Result.success() else Result.failure()
    }
}
