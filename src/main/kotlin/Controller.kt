/* Controller.kt

  Copyright (C) 2020 Eroica

  This software is provided 'as-is', without any express or implied
  warranty.  In no event will the authors be held liable for any damages
  arising from the use of this software.

  Permission is granted to anyone to use this software for any purpose,
  including commercial applications, and to alter it and redistribute it
  freely, subject to the following restrictions:

  1. The origin of this software must not be misrepresented; you must not
     claim that you wrote the original software. If you use this software
     in a product, an acknowledgment in the product documentation would be
     appreciated but is not required.
  2. Altered source versions must be plainly marked as such, and must not be
     misrepresented as being the original software.
  3. This notice may not be removed or altered from any source distribution.

*/

import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.scene.web.WebView
import javafx.stage.Stage
import netscape.javascript.JSObject
import java.nio.file.Files
import java.util.*

object ClipboardManager {
    fun putIntoClipboard(content: String) {
        Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
            putString(content)
        })
    }
}

class MainController {
    @FXML
    lateinit var webView: WebView

    fun initialize() {
        webView.isContextMenuEnabled = false
        webView.engine.load(javaClass.getResource("index.html").toExternalForm())
        webView.engine.loadWorker.stateProperty().addListener { _, _, newValue ->
            if (newValue == Worker.State.SUCCEEDED) {
                val window = webView.engine.executeScript("window") as JSObject
                window.setMember("ClipboardManager", ClipboardManager)
            }
        }
    }

    fun onDragDropped(dragEvent: DragEvent) {
        val img = Base64.getEncoder().encodeToString(Files.readAllBytes(dragEvent.dragboard.files.first().toPath()))
        webView.engine.executeScript("""update("$img");""")
    }

    fun onDragOver(dragEvent: DragEvent) {
        if (dragEvent.dragboard.hasFiles()) {
            dragEvent.acceptTransferModes(*TransferMode.ANY)
        }
        dragEvent.consume()
    }
}
