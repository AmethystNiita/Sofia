package com.amethystniita.sofia

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object AdbEngine {

    fun isBinderAlive(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            isBinderAlive() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        if (isBinderAlive() && !hasPermission()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    fun trimCaches(): Boolean {
        return try {
            if (!hasPermission()) return false

            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true

            val process = newProcessMethod.invoke(
                null,
                arrayOf("pm", "trim-caches", "999G"),
                null,
                null
            ) as Process

            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}