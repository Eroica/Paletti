package app.paletti.lib

object Leptonica {
    external fun posterize(count: Int, isBlackWhite: Boolean, paths: Array<String>): Int
}
