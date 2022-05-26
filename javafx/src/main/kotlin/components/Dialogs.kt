package components

import app.paletti.BuildConfig
import javafx.fxml.FXMLLoader
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.File

abstract class BaseDialog(window: IWindow) : Stage() {
    init {
        isResizable = false
        initOwner(window.stage())
        initModality(Modality.WINDOW_MODAL)

        addEventHandler(KeyEvent.KEY_RELEASED) {
            if (it.code == KeyCode.ESCAPE) {
                close()
            }
        }
    }
}

class AboutDialog(window: IWindow) : BaseDialog(window) {
    init {
        FXMLLoader(javaClass.getResource("Dialog_About.fxml"), null).apply {
            setRoot(this@AboutDialog)
            setController(this@AboutDialog)
            namespace["APP_NAME"] = BuildConfig.APP_NAME
            namespace["APP_VERSION"] = BuildConfig.APP_VERSION
            namespace["APP_COPYRIGHT"] = BuildConfig.APP_COPYRIGHT
            namespace["LICENSE_REPORT"] = """${BuildConfig.APP_LICENSE}

${File(javaClass.classLoader.getResource("LICENSE").toURI()).readText()}

${File(javaClass.classLoader.getResource("leptonica.txt").toURI()).readText()}

${File(javaClass.classLoader.getResource("licenseReport.txt").toURI()).readText()}"""
            load()
        }

        scene?.stylesheets?.add("style.css")
    }
}
