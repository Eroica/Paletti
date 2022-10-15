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
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.sql.SQLException

const val APP_NAME = "Paletti"
const val DB_NAME = "Paletti.db"
const val APP_WEBSITE = "https://paletti.app"

fun main(args: Array<String>) {
    System.loadLibrary("Paletti")
    System.setProperty("prism.lcdtext", "false")
    Application.launch(Paletti::class.java, *args)
}

class Paletti : Application() {
    companion object {
        lateinit var App: Application
    }

    override fun start(primaryStage: Stage) {
        Paletti.App = this
        val cacheDir = File(AppDirsFactory.getInstance().getUserCacheDir(APP_NAME, null, null))
        cacheDir.mkdirs()
        val database = Database(cacheDir.resolve(DB_NAME), cacheDir)
        val images = SqlImages(database)

        var viewModelId = database.monotonicId()
        if (!database.isRestoreImage) {
            viewModelId++
        }

        val viewModel = ViewModel(viewModelId, images, cacheDir.resolve(DB_NAME).toString(), cacheDir)
        viewModel.isRestoreImage.addListener(InvalidationListener {
            database.isRestoreImage = viewModel.isRestoreImage.get()
        })

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
            viewModel.onDestroy()
            database.close()
        }

        if (database.isRestoreImage) {
            try {
                viewModel.count.set(images[viewModelId].count)
                viewModel.isBlackWhite.set(images[viewModelId].isBlackWhite)
                viewModel.isRestoreImage.set(true)
                viewModel.load(images[viewModelId].source)
            } catch (e: SQLException) {
                database.isRestoreImage = false
            }
        } else {
            if (parameters.unnamed.isNotEmpty()) {
                viewModel.count.value = parameters.named.getOrDefault("colors", "6").toInt()
                viewModel.isBlackWhite.value = parameters.named.getOrDefault("bw", "false").toBoolean()
                viewModel.load(parameters.unnamed.first())
            }
        }

        primaryStage.title = "Paletti"
        primaryStage.show()
        Windows.subclass(primaryStage.title)
    }
}
