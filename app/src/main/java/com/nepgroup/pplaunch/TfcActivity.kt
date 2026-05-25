package com.nepgroup.pplaunch

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import com.nepgroup.pplaunch.databinding.ActivityTfcBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import javax.net.ssl.HttpsURLConnection

class TfcActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTfcBinding
    private val TAG = "TfcActivity"
    private lateinit var repo: SettingsRepository
    private lateinit var prefs: Preferences
    private var pollingJob: Job? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTfcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        repo = SettingsRepository(applicationContext)
        lifecycleScope.launch {
            prefs = repo.getAll()
        }

        binding.wvTFC.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            loadUrl(intent.getStringExtra("url") ?: "No Url")
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.wvTFC.canGoBack()) {
                    binding.wvTFC.goBack()
                } else {
                    val builder = AlertDialog.Builder(this@TfcActivity)

                    builder
                        .setTitle(getString(R.string.exit))
                        .setMessage(getString(R.string.exit_message))
                        .setIcon(R.drawable.exit)
                        .setPositiveButton(getString(R.string.yes)) { _, _ ->
                            finish()
                        }.setNegativeButton(getString(R.string.no)) { dialog, _ ->
                            dialog.dismiss()
                        }.setNeutralButton("Go to Loading Screen") { _, _ ->
                            val intent = Intent(this@TfcActivity, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                        }.show()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Activity Resumed, Resuming Polling!")
        repo = SettingsRepository(applicationContext)
        lifecycleScope.launch {
            prefs = repo.getAll()
            binding.wvTFC.setInitialScale(prefs[SettingsKeys.ZOOM_PERCENT]?: 75)
        }
        pollingJob?.cancel()
        startPolling(intent.getStringExtra("url") ?: "No Url")
    }

    override fun onPause() {
        super.onPause()
        Log.w(TAG, "Activity Paused, Stopping Polling!")
        pollingJob?.cancel()
    }

    private fun startPolling(targetURL: String) {
        var lastOk = LocalDateTime.now()

        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(5000)
            while (true) {
                try {
                    val url = URL(targetURL)
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    val status = connection.responseCode
                    if (status >= 400) {
                        connection.errorStream?.bufferedReader()?.readText() ?: ""
                    } else {
                        connection.inputStream.bufferedReader().readText()
                    }

                    connection.disconnect()

                    lastOk = LocalDateTime.now()
                    Log.d(TAG, "<3")
                } catch (e: Exception) {
                    Log.d(TAG, "</3")
                    val dur = Duration.between(lastOk, LocalDateTime.now()).seconds
                    if (dur >= 60) {
                        redirect()
                    } else {
                        Log.w(
                            TAG,
                            "Heartbeat bad for $dur seconds. Going back in ${60 - dur} seconds!!"
                        )
                    }
                }

                delay(5000)
            }
        }
    }


    private fun redirect() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)

    }
}