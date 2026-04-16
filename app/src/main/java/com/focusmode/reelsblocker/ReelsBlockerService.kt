package com.focusmode.reelsblocker

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class ReelsBlockerService : AccessibilityService() {

    private var lastBlockTime = 0L
    private val COOLDOWN_MS = 3000L

    companion object {
        var isRunning = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val now = System.currentTimeMillis()
        if (now - lastBlockTime < COOLDOWN_MS) return

        when (pkg) {
            "com.instagram.android" -> checkAndBlock(event, "Reels", "Instagram Reels bloqueado")
            "com.google.android.youtube" -> checkAndBlock(event, "Shorts", "YouTube Shorts bloqueado")
        }
    }

    private fun checkAndBlock(event: AccessibilityEvent, keyword: String, message: String) {
        // Check activity/fragment class name first (fast path)
        val className = event.className?.toString() ?: ""
        if (className.contains(keyword, ignoreCase = true)) {
            block(message)
            return
        }

        // Scan accessibility tree for a selected nav tab with the keyword
        val root = rootInActiveWindow ?: return
        try {
            if (hasSelectedNode(root, keyword, depth = 0)) {
                block(message)
            }
        } finally {
            root.recycle()
        }
    }

    private fun hasSelectedNode(node: AccessibilityNodeInfo, keyword: String, depth: Int): Boolean {
        if (depth > 10) return false

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val matchesKeyword = text.equals(keyword, ignoreCase = true) ||
                desc.contains(keyword, ignoreCase = true)

        if (matchesKeyword && (node.isSelected || node.isChecked)) return true

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasSelectedNode(child, keyword, depth + 1)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun block(message: String) {
        lastBlockTime = System.currentTimeMillis()
        performGlobalAction(GLOBAL_ACTION_HOME)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {
        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}
