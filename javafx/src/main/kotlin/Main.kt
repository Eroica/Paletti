import app.paletti.lib.Windows
import components.IWindow
import components.PalettiActivity
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.Dispatchers

const val APP_NAME = "Paletti"
const val DB_NAME = "Paletti.db"
const val APP_WEBSITE = "https://paletti.app"

fun main(args: Array<String>) {
    System.loadLibrary("Paletti")
    System.setProperty("prism.lcdtext", "false")

    if (!Windows.isAMDGPU()) {
        System.setProperty("prism.forceUploadingPainter", "true")
        System.setProperty("javafx.animation.fullspeed", "true")
    }

    Application.launch(Paletti::class.java, *args)
}

class Paletti : Application() {
    companion object {
        lateinit var App: Application
    }

    private val appDir: AppDir by lazy { AppDir() }

    override fun init() {
        super.init()
        Paletti.App = this

        if (appDir.database.isPrefersDarkMode()) {
            setUserAgentStylesheet("/FluentDark.css")
        } else {
            setUserAgentStylesheet("/Fluent.css")
        }
    }

    override fun start(primaryStage: Stage) {
        var viewModelId = appDir.database.monotonicId()
        if (!appDir.database.isRestoreImage) {
            viewModelId++
        }

        val viewModel = ViewModel(viewModelId, SqlImages(appDir.database), appDir, Dispatchers.IO)
        val activity = PalettiActivity(viewModel, object : IWindow {
            override fun close() {
                primaryStage.close()
            }

            override fun stage(): Stage {
                return primaryStage
            }
        })

        val scene = Scene(activity)
        scene.fill = Color.TRANSPARENT
        scene.stylesheets += "style.css"

        primaryStage.initStyle(StageStyle.UNIFIED)
        primaryStage.minWidth = 500.0
        primaryStage.minHeight = 450.0
        primaryStage.scene = scene
        primaryStage.icons += Image(javaClass.getResourceAsStream("icons/256.png"))
        primaryStage.setOnCloseRequest {
            activity.onDestroy()
            viewModel.onCleared()
            appDir.database.close()
        }

        if (appDir.database.isPrefersDarkMode()) {
            primaryStage.scene.root.styleClass.add("paletti-is-dark")
        }

        primaryStage.title = APP_NAME
        primaryStage.show()
        Windows.subclass(primaryStage.title, appDir.database.isPrefersDarkMode())
    }
}
