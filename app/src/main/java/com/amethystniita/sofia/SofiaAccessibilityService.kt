package com.amethystniita.sofia

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SofiaAccessibilityService : AccessibilityService() {

    companion object {
        var isRunning = false
        var isCleaningPaused = false
        var packageList: List<String> = emptyList()
        var currentPackageIndex = 0
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessingApp = false
    private var isAttemptingClear = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRunning || isCleaningPaused || currentPackageIndex >= packageList.size || isProcessingApp) return

        val rootNode = rootInActiveWindow ?: return

        
        val storageSystemText = getSystemSettingsString(
            listOf("storage_settings", "storage_label", "storage_settings_title", "storage_use")
        )

        val storageNode = findNodeByTextOrViewId(
            root = rootNode,
            targetText = storageSystemText,
            targetViewIdKeywords = listOf("storage")
        )

        if (storageNode != null) {
            if (performClickOnNodeOrParent(storageNode)) {
                isProcessingApp = true
                handler.postDelayed({ checkAndClearCacheWithRetries(attempt = 1) }, 500)
            }
        }
    }

    private fun checkAndClearCacheWithRetries(attempt: Int) {
        if (isAttemptingClear) return
        val rootNode = rootInActiveWindow

        if (rootNode == null) {
            if (attempt < 3) {
                handler.postDelayed({ checkAndClearCacheWithRetries(attempt + 1) }, 400)
            } else {
                advanceToNextApp()
            }
            return
        }

        
        val clearCacheSystemText = getSystemSettingsString(
            listOf("clear_cache_btn_text", "clear_cache", "storage_clear_cache_btn_text", "clear_cache_title")
        )

        val clearCacheNode = findNodeByTextOrViewId(
            root = rootNode,
            targetText = clearCacheSystemText,
            targetViewIdKeywords = listOf("clear_cache", "button2", "right_button")
        )

        if (clearCacheNode != null) {
            if (clearCacheNode.isEnabled) {
                isAttemptingClear = true
                performClickOnNodeOrParent(clearCacheNode)
                handler.postDelayed({ advanceToNextApp() }, 300)
                return
            }
        }

        
        if (attempt < 3) {
            handler.postDelayed({ checkAndClearCacheWithRetries(attempt + 1) }, 400)
        } else {
            advanceToNextApp()
        }
    }

    
    private fun getSystemSettingsString(resourceNames: List<String>): String? {
        return try {
            val settingsContext = createPackageContext("com.android.settings", Context.CONTEXT_IGNORE_SECURITY)
            val res = settingsContext.resources
            val packageName = "com.android.settings"

            for (name in resourceNames) {
                val resId = res.getIdentifier(name, "string", packageName)
                if (resId != 0) {
                    val resolvedString = res.getString(resId)
                    if (resolvedString.isNotBlank()) {
                        return resolvedString
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    
    private fun findNodeByTextOrViewId(
        root: AccessibilityNodeInfo,
        targetText: String?,
        targetViewIdKeywords: List<String>
    ): AccessibilityNodeInfo? {
        val queue = mutableListOf(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)

            
            val nodeText = node.text?.toString() ?: node.contentDescription?.toString()
            if (!targetText.isNullOrBlank() && !nodeText.isNullOrBlank()) {
                if (nodeText.contains(targetText, ignoreCase = true)) {
                    return node
                }
            }

            
            val viewId = node.viewIdResourceName
            if (viewId != null) {
                for (keyword in targetViewIdKeywords) {
                    if (viewId.contains(keyword, ignoreCase = true)) {
                        return node
                    }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var tempNode = node
        while (tempNode != null) {
            if (tempNode.isClickable && tempNode.isEnabled) {
                return tempNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            tempNode = tempNode.parent
        }
        return false
    }

    private fun advanceToNextApp() {
        isProcessingApp = false
        isAttemptingClear = false
        currentPackageIndex++

        FloatingControlService.updateProgress(currentPackageIndex, packageList.size)

        if (currentPackageIndex < packageList.size) {
            val packageName = packageList[currentPackageIndex]
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        } else {
            
            isRunning = false

            
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (launchIntent != null) {
                startActivity(launchIntent)
            }

            
            handler.postDelayed({
                android.widget.Toast.makeText(applicationContext, "Cache cleaning completed!", android.widget.Toast.LENGTH_SHORT).show()
                stopService(Intent(this, FloatingControlService::class.java))
            }, 100)
        }
    }

    override fun onInterrupt() {}
}