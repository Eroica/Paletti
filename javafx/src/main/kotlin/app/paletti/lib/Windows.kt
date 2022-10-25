package app.paletti.lib

import java.io.BufferedReader
import java.io.InputStreamReader

object Windows {
    external fun subclass(target: String)

    fun isAMDGPU(): Boolean {
        val p = Runtime.getRuntime().exec(arrayOf("wmic", "PATH", "Win32_videocontroller", "GET", "description"))
        val output = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
        return "AMD" in output.uppercase()
    }
}
