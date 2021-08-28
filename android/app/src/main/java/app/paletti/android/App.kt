package app.paletti.android

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun bindFilePaths(@ApplicationContext appContext: Context): FilePaths {
        return FilePaths(
            appContext.cacheDir.resolve("Colors.txt"),
            appContext.cacheDir.resolve("tmp.dat"),
            appContext.cacheDir.resolve("tmp.bmp"),
            appContext.cacheDir.resolve("out.bmp"),
            appContext.cacheDir.resolve("Palette.png")
        )
    }

    @Singleton
    @Provides
    fun provideWorker(@ApplicationContext appContext: Context): WorkManager {
        return WorkManager.getInstance(appContext)
    }
}

@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("paletti")
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
