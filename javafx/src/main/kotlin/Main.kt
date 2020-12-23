import components.PalettiWindow
import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage

fun main(args: Array<String>) {
    Application.launch(Paletti::class.java, *args)
}

class Paletti : Application() {
    override fun start(primaryStage: Stage) {
        val viewModel = ImageViewModel()
        val stage = PalettiWindow(viewModel)
        stage.icons += Image(javaClass.getResourceAsStream("icons/256.png"))
        stage.focusedProperty().addListener { _, _, hasFocus ->
            if (hasFocus) {
                stage.scene.root.styleClass.remove("paletti-no-focus")
            } else {
                stage.scene.root.styleClass.add("paletti-no-focus")
            }
        }
        stage.show()
    }
}
