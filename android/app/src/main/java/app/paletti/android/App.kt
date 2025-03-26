package app.paletti.android

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.conf.global
import java.io.File

data class FilePaths(
    val colors: File,
    val copiedImage: File,
    val tmpImage: File,
    val outImage: File,
    val palette: File
)

object ProviderData {
    const val provider = "app.paletti.android.provider"
    const val type = "type/image"
}

fun appModule(appContext: Context) = DI.Module(name = "App") {
    bindSingleton {
        FilePaths(
            appContext.cacheDir.resolve("Colors.txt"),
            appContext.cacheDir.resolve("tmp.dat"),
            appContext.cacheDir.resolve("tmp.bmp"),
            appContext.cacheDir.resolve("out.bmp"),
            appContext.cacheDir.resolve("Palette.png")
        )
    }
    bindSingleton { WorkManager.getInstance(appContext) }
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("paletti")
        DI.global.addImport(appModule(applicationContext))
    }
}
