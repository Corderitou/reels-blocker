package com.focusmode.reelsblocker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.focusmode.reelsblocker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val active = isAccessibilityServiceEnabled()
        if (active) {
            binding.tvStatus.text = "ACTIVO ✓"
            binding.tvStatus.setTextColor(getColor(R.color.status_active))
            binding.tvDescription.text =
                "El bloqueador está funcionando.\n\nCada vez que abras Reels en Instagram o Shorts en YouTube, la app te regresará automáticamente al inicio."
            binding.btnSettings.text = "Ir a configuración de accesibilidad"
        } else {
            binding.tvStatus.text = "INACTIVO"
            binding.tvStatus.setTextColor(getColor(R.color.status_inactive))
            binding.tvDescription.text =
                "El bloqueador no está activo.\n\nToca el botón, busca \"Bloqueador de Reels\" en la lista y actívalo."
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
