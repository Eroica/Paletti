package app.paletti.lib

object Leptonica {
    const val OK = 0

    @Deprecated("Use posterize2 with path to database file")
    external fun posterize(count: Int, isBlackWhite: Boolean, paths: Array<String>): Int

    external fun posterize2(imageId: Int, databasePath: String): Int
}
