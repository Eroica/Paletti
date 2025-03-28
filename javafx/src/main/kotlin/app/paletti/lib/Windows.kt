package app.paletti.lib

import java.io.BufferedReader
import java.io.InputStreamReader

object Windows {
    external fun isdarkmode(): Boolean
    external fun subclass(target: String, isSetDarkMode: Boolean)

    fun isAMDGPU(): Boolean {
        val process = Runtime.getRuntime().exec(
            arrayOf(
                "powershell",
                "-command",
                """"(Get-WmiObject -class Win32_VideoController -Property Description).Description""""
            )
        )
        val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        return "AMD" in output.uppercase()
    }
}
