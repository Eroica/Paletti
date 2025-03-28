import javafx.application.Application
import javafx.application.HostServices

const val APP_NAME = "Paletti"
const val DB_NAME = "Paletti.db"
const val APP_WEBSITE = "https://paletti.app"

class AppModule(app: Application) {
    val hostServices: HostServices = app.hostServices
    val appDir = AppDir()
}
