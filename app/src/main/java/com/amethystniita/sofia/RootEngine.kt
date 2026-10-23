package com.amethystniita.sofia

import java.io.DataOutputStream

object RootEngine {
    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllCaches(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("rm -rf /data/data/*/cache/*\n")
            os.writeBytes("rm -rf /data/data/*/code_cache/*\n")
            os.writeBytes("rm -rf /sdcard/Android/data/*/cache/*\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}