package com.nepgroup.pplaunch

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.nepgroup.pplaunch.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity() {
private lateinit var binding: ActivitySettingsBinding
private lateinit var repo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repo = SettingsRepository(applicationContext)

        val redirectModes = listOf(
            "TFC",
            "Custom URL"
        )
        val redirectAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, redirectModes)
        binding.actvRedirectMode.setAdapter(redirectAdapter)

        val urlPrefixes = listOf(
            "CONTROL",
            "NEXT"
        )
        val urlPrefixAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, urlPrefixes)
        binding.actvUrlPrefix.setAdapter(urlPrefixAdapter)

        val urlBases = listOf(
            "NEPGroup.io",
            "TFCLabs.com"
        )
        val urlBaseAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, urlBases)
        binding.actvUrlBase.setAdapter(urlBaseAdapter)

        lifecycleScope.launch {
            val prefs = repo.getAll()
            val savedRedirect = prefs[SettingsKeys.REDIRECT_MODE] ?: redirectModes.first()
            binding.actvRedirectMode.setText(savedRedirect, false)
            setRedirectModeVisibility(savedRedirect)

            binding.actvUrlPrefix.setText(prefs[SettingsKeys.URL_PREFIX] ?: urlPrefixes.first(), false)
            binding.actvUrlBase.setText(prefs[SettingsKeys.BASE_URL] ?: urlBases.first(), false)
            binding.etTFCInstance.setText(prefs[SettingsKeys.TFC_INSTANCE] ?: "")
            binding.etCustomURL.setText(prefs[SettingsKeys.CUSTOM_URL] ?: "")
            binding.etLoadingString.setText(prefs[SettingsKeys.LOADING_STRING] ?: "")
            binding.cbDebugMode.isChecked = prefs[SettingsKeys.DEBUG] ?: false

            val savedZoom = prefs[SettingsKeys.ZOOM_PERCENT] ?: 75
            binding.etZoomPercent.setText(savedZoom.toString())
        }


        binding.actvRedirectMode.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as String
            setRedirectModeVisibility(selected)
        }
        binding.btnSaveSettings.setOnClickListener {
            lifecycleScope.launch {
                saveSettings()
                finish()
            }
        }

    }

    suspend fun saveSettings(){
        Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        dataStore.edit { prefs ->
            prefs[SettingsKeys.DEBUG]           = binding.cbDebugMode.isChecked
            prefs[SettingsKeys.REDIRECT_MODE]   = binding.actvRedirectMode.text.toString()
            prefs[SettingsKeys.URL_PREFIX]      = binding.actvUrlPrefix.text.toString()
            prefs[SettingsKeys.TFC_INSTANCE]    = binding.etTFCInstance.text.toString().trim()
            prefs[SettingsKeys.BASE_URL]        = binding.actvUrlBase.text.toString()
            prefs[SettingsKeys.CUSTOM_URL]      = binding.etCustomURL.text.toString().trim()
            prefs[SettingsKeys.LOADING_STRING]  = binding.etLoadingString.text.toString().trim()
            prefs[SettingsKeys.ZOOM_PERCENT]    = binding.etZoomPercent.text.toString().trim().toInt()
        }
    }

    private fun setRedirectModeVisibility(selected: String){
        if (selected == "TFC") {
            binding.tvTFCInstance.visibility = View.VISIBLE
            binding.llTFCInstance.visibility = View.VISIBLE

            binding.tvCustomURL.visibility = View.GONE
            binding.tvLoadingString.visibility = View.GONE
            binding.tilCustomURL.visibility = View.GONE
            binding.llLoadingString.visibility = View.GONE
        } else if (selected == "Custom URL") {
            binding.tvTFCInstance.visibility = View.GONE
            binding.llTFCInstance.visibility = View.GONE

            binding.tvCustomURL.visibility = View.VISIBLE
            binding.tvLoadingString.visibility = View.VISIBLE
            binding.tilCustomURL.visibility = View.VISIBLE
            binding.llLoadingString.visibility = View.VISIBLE
        }
    }
}