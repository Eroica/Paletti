package app.paletti.lib

object Leptonica {
    const val OK = 0

    init {
        System.loadLibrary("Paletti")
    }

    external fun posterize(count: Int, isBlackWhite: Boolean, paths: Array<String>): Int

    external fun posterize2(imageId: Int, databasePath: String): Int
}
