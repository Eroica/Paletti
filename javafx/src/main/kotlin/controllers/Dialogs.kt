package controllers

import APP_WEBSITE
import Paletti
import app.paletti.BuildConfig
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Modality
import javafx.stage.Stage

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
    private val licenseBuilder: StringBuilder by lazy {
        val licenseBuilder = StringBuilder()
        listOfNotNull(
            javaClass.classLoader.getResourceAsStream("LICENSE"),
            javaClass.classLoader.getResourceAsStream("vendor/leptonica.txt"),
            javaClass.classLoader.getResourceAsStream("vendor/licenseReport.txt"),
            javaClass.classLoader.getResourceAsStream("vendor/apache.txt"),
            javaClass.classLoader.getResourceAsStream("vendor/slf4j.txt"),
            javaClass.classLoader.getResourceAsStream("vendor/sqlite_modern_cpp.txt")
        ).forEach {
            licenseBuilder.append(it.bufferedReader().use { it.readText() })
            licenseBuilder.append("\n\n")
        }
        licenseBuilder
    }

    init {
        FXMLLoader(javaClass.getResource("Dialog_About.fxml"), null).apply {
            setRoot(this@AboutDialog)
            setController(this@AboutDialog)
            namespace["APP_NAME"] = BuildConfig.APP_NAME
            namespace["APP_VERSION"] = BuildConfig.APP_VERSION
            namespace["APP_COPYRIGHT"] = BuildConfig.APP_COPYRIGHT
            namespace["LICENSE_REPORT"] = """${BuildConfig.APP_LICENSE}

$licenseBuilder"""
            load()
        }

        scene?.stylesheets?.add("style.css")
    }

    @FXML
    private fun onWebsiteClick(event: ActionEvent) {
        Paletti.AppModule.hostServices.showDocument(APP_WEBSITE)
        event.consume()
    }
}
