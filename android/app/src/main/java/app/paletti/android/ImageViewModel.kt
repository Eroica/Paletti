package app.paletti.android

import android.graphics.Color
import android.net.Uri
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class ImageViewModel(
    private val colorsFile: File,
    private val workManager: WorkManager
) : ViewModel() {
    val count = ObservableFloat(6f)
    val colors = ObservableArrayList<Int>()
    val isBlackWhite = ObservableBoolean(false)
    val imageId = ObservableInt(-1)
    val workState = MediatorLiveData<WorkInfo.State>()

    private var time: Long = 0

    init {
        workState.addSource(workManager.getWorkInfosForUniqueWorkLiveData(PosterizeWorker.WORK_NAME)) { workStates ->
            if (!workStates.isNullOrEmpty()) {
                when {
                    workStates.all { it.state == WorkInfo.State.SUCCEEDED } -> {
                        readColors()
                        workState.value = WorkInfo.State.SUCCEEDED
                    }
                    workStates.any { it.state == WorkInfo.State.RUNNING } -> {
                        workState.value = WorkInfo.State.RUNNING
                    }
                    workStates.any { it.state == WorkInfo.State.FAILED } -> {
                        workState.value = WorkInfo.State.FAILED
                    }
                }
            }
        }
    }

    fun new(uri: Uri) {
        val data = Data.Builder().apply {
            putString(CopyWorker.URI, uri.toString())
            putInt(PosterizeWorker.COUNT, count.get().toInt())
            putBoolean(PosterizeWorker.IS_BLACK_WHITE, isBlackWhite.get())
        }.build()
        val copy = OneTimeWorkRequestBuilder<CopyWorker>()
            .setInputData(data)
            .build()
        val posterize = OneTimeWorkRequestBuilder<PosterizeWorker>()
            .setInputData(data)
            .build()
        workManager.beginUniqueWork(PosterizeWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, copy)
            .then(posterize)
            .enqueue()
    }

    fun posterize(count: Float, isBlackWhite: Boolean) {
        posterize(count.toInt(), isBlackWhite)
    }

    fun posterize(count: Int, isBlackWhite: Boolean) {
        time = System.nanoTime()
        viewModelScope.launch {
            delay(100)
            val delta = System.nanoTime()
            // Difference in NANOseconds
            if (delta - time >= 100000000) {
                val builder = Data.Builder()
                builder.putInt(PosterizeWorker.COUNT, count)
                builder.putBoolean(PosterizeWorker.IS_BLACK_WHITE, isBlackWhite)
                val data = builder.build()
                workManager.enqueueUniqueWork(
                    PosterizeWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<PosterizeWorker>().setInputData(data).build()
                )
            }
        }
    }

    private fun readColors() {
        val currentCount = colors.size
        var i = 0
        colorsFile.forEachLine {
            val rgb = it.split(",").map { it.toInt() }
            try {
                colors[i] = Color.rgb(rgb[0], rgb[1], rgb[2])
            } catch (_: IndexOutOfBoundsException) {
                colors.add(Color.rgb(rgb[0], rgb[1], rgb[2]))
            } finally {
                i++
            }
        }
        (i until currentCount).forEach { _ -> colors.removeLast() }
    }
}
