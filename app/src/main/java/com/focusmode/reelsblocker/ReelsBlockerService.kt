package com.focusmode.reelsblocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
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
            "com.instagram.android" -> checkAndBlock(event, "Reels", instagram = true)
            "com.google.android.youtube" -> checkAndBlock(event, "Shorts", instagram = false)
        }
    }

    private fun checkAndBlock(event: AccessibilityEvent, keyword: String, instagram: Boolean) {
        val className = event.className?.toString() ?: ""
        if (className.contains(keyword, ignoreCase = true)) {
            block(instagram)
            return
        }

        val root = rootInActiveWindow ?: return
        try {
            if (hasSelectedNode(root, keyword, depth = 0)) {
                block(instagram)
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

    private fun block(instagram: Boolean) {
        lastBlockTime = System.currentTimeMillis()
        if (instagram) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("instagram://direct/inbox"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        Handler(Looper.getMainLooper()).post {
            val msg = if (instagram) "→ Mensajes de Instagram" else "YouTube Shorts bloqueado"
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
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
