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
        val closeListener = {
            stage.onDestroy()
            viewModel.onDestroy()
            database.close()
        }
        stage.closeRequest = closeListener
        stage.setOnCloseRequest { closeListener() }
        if (parameters.unnamed.isNotEmpty()) {
            viewModel.count.value = parameters.named.getOrDefault("colors", "6").toInt()
            viewModel.isBlackWhite.value = parameters.named.getOrDefault("bw", "false").toBoolean()
            viewModel.load(parameters.unnamed.first())
        }
        stage.show()
    }
}
