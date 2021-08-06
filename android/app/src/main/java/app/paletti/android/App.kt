package app.paletti.android

import android.app.Application
import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
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

val appModule = module {
    single { WorkManager.getInstance(androidContext()) }
    single {
        androidContext().run {
            FilePaths(
                cacheDir.resolve("Colors.txt"),
                cacheDir.resolve("tmp.dat"),
                cacheDir.resolve("tmp.bmp"),
                cacheDir.resolve("out.bmp"),
                cacheDir.resolve("Palette.png")
            )
        }
    }
    viewModel { ImageViewModel(get<FilePaths>().colors, get()) }
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("paletti")
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }
}
