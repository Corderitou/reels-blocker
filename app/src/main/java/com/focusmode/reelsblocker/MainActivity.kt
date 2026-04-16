package com.focusmode.reelsblocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.focusmode.reelsblocker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val PREFS_NAME        = "blocker_settings"
        const val KEY_BLOCK_REELS   = "block_reels"
        const val KEY_BLOCK_STORIES = "block_stories"
        const val KEY_BLOCK_HOME    = "block_home_feed"
        const val KEY_BLOCK_SHORTS  = "block_shorts"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        loadSwitchStates()
        attachSwitchListeners()
    }

    override fun onResume() {
        super.onResume()
        loadSwitchStates()
        updateStatus()
    }

    private fun loadSwitchStates() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        binding.switchReels.isChecked   = prefs.getBoolean(KEY_BLOCK_REELS,   true)
        binding.switchStories.isChecked = prefs.getBoolean(KEY_BLOCK_STORIES, false)
        binding.switchHome.isChecked    = prefs.getBoolean(KEY_BLOCK_HOME,    false)
        binding.switchShorts.isChecked  = prefs.getBoolean(KEY_BLOCK_SHORTS,  true)
    }

    private fun attachSwitchListeners() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        binding.switchReels.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_BLOCK_REELS, checked).apply()
            updateStatus()
        }
        binding.switchStories.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_BLOCK_STORIES, checked).apply()
            updateStatus()
        }
        binding.switchHome.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_BLOCK_HOME, checked).apply()
            updateStatus()
        }
        binding.switchShorts.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_BLOCK_SHORTS, checked).apply()
            updateStatus()
        }
    }

    private fun updateStatus() {
        val active = isAccessibilityServiceEnabled()
        binding.tvStatus.text = if (active) "ACTIVO ✓" else "INACTIVO"
        binding.tvStatus.setTextColor(
            getColor(if (active) R.color.status_active else R.color.status_inactive)
        )

        if (active) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val items = buildList {
                if (prefs.getBoolean(KEY_BLOCK_REELS,   true))  add("Reels")
                if (prefs.getBoolean(KEY_BLOCK_STORIES, false)) add("Historias")
                if (prefs.getBoolean(KEY_BLOCK_HOME,    false)) add("Feed de inicio")
                if (prefs.getBoolean(KEY_BLOCK_SHORTS,  true))  add("YouTube Shorts")
            }
            binding.tvDescription.text = if (items.isEmpty())
                "Bloqueador activo pero sin nada seleccionado. Activa al menos una opción abajo."
            else
                "Bloqueando: ${items.joinToString(", ")}.\n\nInstagram te manda al Direct. YouTube te manda al inicio."
            binding.btnSettings.text = "Configuración de accesibilidad"
        } else {
            binding.tvDescription.text =
                "El bloqueador no está activo.\n\nToca el botón, busca \"Bloqueador de Reels\" y actívalo."
            binding.btnSettings.text = "Activar servicio"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${ReelsBlockerService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(service, ignoreCase = true)) return true
        }
        return false
    }
}
