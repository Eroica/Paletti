import app.paletti.lib.Windows
import controllers.IWindow
import controllers.PalettiActivity
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.Dispatchers

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
        lateinit var AppModule: AppModule
    }

    override fun init() {
        super.init()
        AppModule = AppModule(this)
        if (AppModule.appDir.database.isPrefersDarkMode()) {
            setUserAgentStylesheet("/FluentDark.css")
        } else {
            setUserAgentStylesheet("/Fluent.css")
        }
    }

    override fun start(primaryStage: Stage) {
        var viewModelId = AppModule.appDir.database.monotonicId()
        if (!AppModule.appDir.database.isRestoreImage) {
            viewModelId++
        }

        val viewModel = ViewModel(
            viewModelId, SqlImages(AppModule.appDir.database), AppModule.appDir, Dispatchers.IO
        )
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
            AppModule.appDir.database.close()
        }

        if (AppModule.appDir.database.isPrefersDarkMode()) {
            primaryStage.scene.root.styleClass.add("paletti-is-dark")
        }

        primaryStage.title = APP_NAME
        primaryStage.show()
        Windows.subclass(primaryStage.title, AppModule.appDir.database.isPrefersDarkMode())
    }
}
