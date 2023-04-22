import app.paletti.lib.Windows
import components.IWindow
import components.PalettiActivity
import javafx.application.Application
import javafx.beans.InvalidationListener
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.sql.SQLException

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

    private val cacheDir = File(AppDirsFactory.getInstance().getUserCacheDir(APP_NAME, null, null))
    private val database by lazy { Database(cacheDir.resolve(DB_NAME), cacheDir) }

    override fun init() {
        super.init()
        Paletti.App = this
        cacheDir.mkdirs()

        if (Windows.isdarkmode() || database.isAlwaysDarkMode) {
            setUserAgentStylesheet("/FluentDark.css")
        } else {
            setUserAgentStylesheet("/Fluent.css")
        }
    }

    override fun start(primaryStage: Stage) {
        val images = SqlImages(database)

        var viewModelId = database.monotonicId()
        if (!database.isRestoreImage) {
            viewModelId++
        }

        val viewModel = ViewModel(viewModelId, images, cacheDir.resolve(DB_NAME).toString(), cacheDir, Dispatchers.IO)
        viewModel.setIsAlwaysDarkMode(database.isAlwaysDarkMode)

        if (database.isRestoreImage) {
            try {
                viewModel.setCount(images[viewModelId].count)
                viewModel.setIsBlackWhite(images[viewModelId].isBlackWhite)
                viewModel.setIsRestoreImage(true)
                runBlocking { viewModel.load(images[viewModelId].source) }
            } catch (e: SQLException) {
                database.isRestoreImage = false
            }
        } else {
            if (parameters.unnamed.isNotEmpty()) {
                viewModel.setCount(parameters.named.getOrDefault("colors", "6").toInt())
                viewModel.setIsBlackWhite(parameters.named.getOrDefault("bw", "false").toBoolean())
                runBlocking { viewModel.load(parameters.unnamed.first()) }
            }
        }

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
        primaryStage.maximizedProperty().addListener { _, _, isMaximized ->
            if (isMaximized) {
                primaryStage.scene.root.styleClass.add("paletti-is-maximized")
            } else {
                primaryStage.scene.root.styleClass.remove("paletti-is-maximized")
            }
        }
        primaryStage.focusedProperty().addListener { _, _, hasFocus ->
            if (hasFocus) {
                primaryStage.scene.root.styleClass.remove("paletti-no-focus")
            } else {
                primaryStage.scene.root.styleClass.add("paletti-no-focus")
            }
        }
        primaryStage.setOnCloseRequest {
            activity.onDestroy()
            viewModel.onCleared()
            database.close()
        }

        if (Windows.isdarkmode() || database.isAlwaysDarkMode) {
            primaryStage.scene.root.styleClass.add("paletti-is-dark")
        }

        viewModel.isRestoreImageProperty().addListener(InvalidationListener {
            database.isRestoreImage = viewModel.getIsRestoreImage()
        })
        viewModel.isAlwaysDarkModeProperty().addListener(InvalidationListener {
            database.isAlwaysDarkMode = viewModel.getIsAlwaysDarkMode()
        })

        primaryStage.title = "Paletti"
        primaryStage.show()
        Windows.subclass(primaryStage.title, Windows.isdarkmode() || database.isAlwaysDarkMode)
    }
}
