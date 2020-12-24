package app.paletti.lib

import java.io.File

data class FilePaths(
    val colors: File,
    val copiedImage: File,
    val tmpImage: File,
    val outImage: File,
    val palette: File
)
