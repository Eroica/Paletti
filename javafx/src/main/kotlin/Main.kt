import components.PalettiWindow
import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage
import net.harawata.appdirs.AppDirsFactory
import java.io.File

const val APP_NAME = "Paletti"
const val DB_NAME = "Paletti.db"

fun main(args: Array<String>) {
    Application.launch(Paletti::class.java, *args)
}

class Paletti : Application() {
    override fun start(primaryStage: Stage) {
        val cacheDir = File(AppDirsFactory.getInstance().getUserCacheDir(APP_NAME, null, null))
        cacheDir.mkdirs()
        val database = Database(cacheDir.resolve(DB_NAME), cacheDir)
        val images = SqlImages(database)
        val viewModel = ViewModel(images, cacheDir.resolve(DB_NAME).toString(), cacheDir)
        val stage = PalettiWindow(viewModel)
        stage.icons += Image(javaClass.getResourceAsStream("icons/256.png"))
        stage.focusedProperty().addListener { _, _, hasFocus ->
            if (hasFocus) {
                stage.scene.root.styleClass.remove("paletti-no-focus")
            } else {
                stage.scene.root.styleClass.add("paletti-no-focus")
            }
        }
        stage.setOnCloseRequest {
            stage.onDestroy()
            viewModel.onDestroy()
            database.close()
        }
        stage.show()
    }
}
