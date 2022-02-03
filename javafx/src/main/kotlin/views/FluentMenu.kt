package views

import javafx.animation.Interpolator
import javafx.animation.TranslateTransition
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.Skin
import javafx.scene.control.skin.ContextMenuSkin
import javafx.util.Duration

class FluentMenu(vararg items: MenuItem) : ContextMenu(*items) {
    override fun show() {
        super.show()
        TranslateTransition(Duration.millis(600.0), scene.root).apply {
            fromY = -height
            toY = 0.0
            interpolator = Interpolator.SPLINE(0.05, 0.965, 0.005, 0.955)
            play()
        }
    }
}
