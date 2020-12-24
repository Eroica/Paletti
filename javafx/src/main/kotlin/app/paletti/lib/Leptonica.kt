package app.paletti.lib

object Leptonica {
    init {
        System.loadLibrary("Paletti")
    }

    external fun posterize(count: Int, isBlackWhite: Boolean, paths: Array<String>): Int
}
