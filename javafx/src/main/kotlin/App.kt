import javafx.application.Application

const val APP_NAME = "Paletti"
const val DB_NAME = "Paletti.db"
const val APP_WEBSITE = "https://paletti.app"

class AppModule(app: Application) {
    val hostServices = app.hostServices
    val appDir = AppDir()
}
