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

        val now = System.currentTimeMillis()
        if (now - lastBlockTime < COOLDOWN_MS) return

        // Catch nav tab clicks early, before the screen loads
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val desc = event.contentDescription?.toString() ?: ""
            if (pkg == "com.instagram.android") {
                val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
                if (prefs.getBoolean(MainActivity.KEY_BLOCK_REELS, true) &&
                    desc.contains("Reels", ignoreCase = true)) {
                    blockInstagram("Reels bloqueado"); return
                }
            } else if (pkg == "com.google.android.youtube") {
                val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
                if (prefs.getBoolean(MainActivity.KEY_BLOCK_SHORTS, true) &&
                    desc.contains("Shorts", ignoreCase = true)) {
                    blockYouTube(); return
                }
            }
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        when (pkg) {
            "com.instagram.android" -> handleInstagram(event)
            "com.google.android.youtube" -> handleYouTube(event)
        }
    }

    private fun handleInstagram(event: AccessibilityEvent) {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val blockReels   = prefs.getBoolean(MainActivity.KEY_BLOCK_REELS, true)
        val blockStories = prefs.getBoolean(MainActivity.KEY_BLOCK_STORIES, false)
        val blockHome    = prefs.getBoolean(MainActivity.KEY_BLOCK_HOME, false)

        if (!blockReels && !blockStories && !blockHome) return

        val className = event.className?.toString() ?: ""
        val root = rootInActiveWindow

        try {
            if (blockReels) {
                if (className.contains("Reels", ignoreCase = true)) {
                    blockInstagram("Reels bloqueado"); return
                }
                if (root != null && hasSelectedNavTab(root, "Reels")) {
                    blockInstagram("Reels bloqueado"); return
                }
            }

            if (blockStories && root != null) {
                if (className.contains("Story", ignoreCase = true) ||
                    hasNodeWithDesc(root, "story", depth = 0)
                ) {
                    blockInstagram("Historia bloqueada"); return
                }
            }

            if (blockHome && root != null) {
                if (hasSelectedNavTab(root, "Home") || hasSelectedNavTab(root, "Inicio")) {
                    blockInstagram("Feed bloqueado"); return
                }
            }
        } finally {
            root?.recycle()
        }
    }

    private fun handleYouTube(event: AccessibilityEvent) {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(MainActivity.KEY_BLOCK_SHORTS, true)) return

        val className = event.className?.toString() ?: ""
        if (className.contains("Shorts", ignoreCase = true)) {
            blockYouTube(); return
        }
        val root = rootInActiveWindow ?: return
        try {
            if (hasSelectedNavTab(root, "Shorts")) blockYouTube()
        } finally {
            root.recycle()
        }
    }

    private fun hasSelectedNavTab(node: AccessibilityNodeInfo, keyword: String, depth: Int = 0): Boolean {
        if (depth > 10) return false
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val matches = text.equals(keyword, ignoreCase = true) ||
                desc.contains(keyword, ignoreCase = true)
        if (matches && (node.isSelected || node.isChecked)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasSelectedNavTab(child, keyword, depth + 1)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun hasNodeWithDesc(node: AccessibilityNodeInfo, keyword: String, depth: Int): Boolean {
        if (depth > 8) return false
        val desc = node.contentDescription?.toString() ?: ""
        if (desc.contains(keyword, ignoreCase = true)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasNodeWithDesc(child, keyword, depth + 1)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun blockInstagram(msg: String) {
        lastBlockTime = System.currentTimeMillis()
        performGlobalAction(GLOBAL_ACTION_BACK)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("instagram://direct/inbox"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun blockYouTube() {
        lastBlockTime = System.currentTimeMillis()
        val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "YouTube Shorts bloqueado", Toast.LENGTH_SHORT).show()
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
