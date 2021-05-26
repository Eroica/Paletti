import components.PalettiWindow
import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage
import net.harawata.appdirs.AppDirsFactory
import java.io.File

fun main(args: Array<String>) {
    Application.launch(Paletti::class.java, *args)
}

class Paletti : Application() {
    override fun start(primaryStage: Stage) {
        val cacheDir = File(AppDirsFactory.getInstance().getUserCacheDir("Paletti", null, null))
        cacheDir.mkdirs()
        val database = Database(cacheDir.resolve("Paletti.db"), cacheDir)
        val images = SqlImages(database)
        val viewModel = ViewModel(images, cacheDir.resolve("Paletti.db").toString(), cacheDir)
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
