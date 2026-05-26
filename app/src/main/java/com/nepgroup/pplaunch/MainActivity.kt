package com.nepgroup.pplaunch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.nepgroup.pplaunch.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import javax.net.ssl.HttpsURLConnection
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    val gitRepo = "brianwlynch/pplaunch-android"
    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: SettingsRepository
    @Suppress("PrivatePropertyName")
    private var TAG: String = "MainActivity"
    private var pollingJob: Job? = null
    private var pollingUpdateJob: Job? = null
    private lateinit var prefs: Preferences
    private var subtitleIndex = -1
    private val bootTime = LocalDateTime.now()
    private var versionName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val packageInfo =
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        versionName = packageInfo.versionName.toString()

        repo = SettingsRepository(applicationContext)
        lifecycleScope.launch {
            prefs = repo.getAll()
            val url = repo.buildURL(prefs) ?: ""
            val instance = prefs[SettingsKeys.TFC_INSTANCE] ?: ""
            val customURL = prefs[SettingsKeys.CUSTOM_URL] ?: ""


            val tfcReady = prefs[SettingsKeys.REDIRECT_MODE] == "TFC" && instance.isNotBlank()
            val customReady = prefs[SettingsKeys.REDIRECT_MODE] == "Custom URL" && customURL.isNotBlank()

            if (tfcReady || customReady){
                startPolling(url, prefs)
                binding.ivAlert.visibility = View.GONE
            }else {
                Log.w("$TAG:Polling", "Settings incomplete, polling stopped!")
                binding.ivAlert.visibility = View.VISIBLE
                binding.ivAlert.setOnClickListener {
                    alertDialog(
                        getString(R.string.settings_incomplete),
                        getString(R.string.complete_settings),
                        icon = R.drawable.settings,
                        "Open Settings" to { openSettings() }
                    )
                }

            }

            checkUpdate()
        }

        binding.ivSettings.setOnClickListener { openSettings() }
        binding.ivHelp.setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }

        animateTextColor(binding.tvTitle)

        lifecycleScope.launch {
            delay(30_000L)
            while (isActive) {
                delay(10_000)
                scrollText(binding.tvSubtitle, prefs)
            }
        }

        val ivRocket = binding.ivRocket
        animateRocket(ivRocket)

    }

    override fun onResume() {
        super.onResume()
        Log.i("$TAG:Polling", "Activity Resumed, Resuming Polling!")

        // This is the same as onCreate, except updating UI here to give time for settings to initially load.
        lifecycleScope.launch {
            lifecycleScope.launch {
                prefs = repo.getAll()
                val url = repo.buildURL(prefs) ?: ""

                val instance = prefs[SettingsKeys.TFC_INSTANCE] ?: ""
                val customURL = prefs[SettingsKeys.CUSTOM_URL] ?: ""

                val tfcReady = prefs[SettingsKeys.REDIRECT_MODE] == "TFC" && instance.isNotBlank()
                val customReady = prefs[SettingsKeys.REDIRECT_MODE] == "Custom URL" && customURL.isNotBlank()

                if (tfcReady || customReady){
                    startPolling(url, prefs)
                    binding.ivAlert.visibility = View.GONE
                } else {
                    Log.w("$TAG:Polling", "Settings incomplete, polling stopped!")
                    Log.w("$TAG:Polling", "Settings incomplete, polling stopped!")
                    binding.ivAlert.visibility = View.VISIBLE
                    binding.ivAlert.setOnClickListener {
                        alertDialog(
                            getString(R.string.settings_incomplete),
                            getString(R.string.complete_settings),
                            icon = R.drawable.settings,
                            "Open Settings" to { openSettings() }
                        )
                    }
                }
                updateUI()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.w("$TAG:Polling", "Activity Paused, Stopping Polling!")
        pollingJob?.cancel()
    }

    /** Update UI from settings */
    fun updateUI() {
        binding.tvTitle.text = if (prefs[SettingsKeys.REDIRECT_MODE] == "Custom URL") {
            val label = prefs[SettingsKeys.LOADING_STRING].takeUnless { it.isNullOrBlank() }
                ?: getString(R.string.custom_url)
            "$label ${getString(R.string.is_launching)}"
        } else {
            getString(R.string.tfc_is_launching)
        }
        binding.tvVersion.text = getString(R.string.pplaunch_v, versionName)

        if (prefs[SettingsKeys.DEBUG] ?: false) {
            binding.ivDebug.visibility = View.VISIBLE
            binding.ivDebug.setOnClickListener {
                alertDialog(
                    "Debug Mode",
                    "You are in debug mode! The panel won't redirect!",
                    R.drawable.debug,
                    "Open Settings" to { openSettings() }
                )
            }
        } else {
            binding.ivDebug.visibility = View.GONE
        }

        if (prefs[SettingsKeys.TFC_INSTANCE].isNullOrBlank() && prefs[SettingsKeys.REDIRECT_MODE] == "TFC") {
            alertDialog(
                getString(R.string.url_error), getString(R.string.no_tfc), R.drawable.link_error,
                "Open Settings" to { openSettings() }
            )

        }
    }

    /** Used to flash the TFC is loading message */
    fun animateTextColor(textView: TextView) {
        val colorStart = "#f0f0ff".toColorInt()
        val colorMiddle = "#b0b0bf".toColorInt()

        val colorAnimator = ValueAnimator.ofArgb(
            colorStart, colorMiddle, colorStart
        ).apply {
            duration = 6000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                textView.setTextColor(it.animatedValue as Int)
            }
        }

        textView.alpha = 0f

        val fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f).apply {
            duration = 1750L
            interpolator = AccelerateDecelerateInterpolator()
        }

        colorAnimator.start()
        fadeIn.start()
    }

    /** Change subtitle messages **/
    fun scrollText(textView: TextView, prefs: Preferences) {
        var messages: List<String>
        val allMessages = arrayOf(
            getString(R.string.subtitle01),
            getString(R.string.subtitle02),
            getString(R.string.subtitle03),
            getString(R.string.subtitle04),
            getString(R.string.subtitle05),
            getString(R.string.subtitle06),
            getString(R.string.subtitle07),
            getString(R.string.subtitle08),
            getString(R.string.subtitle09),
            getString(R.string.subtitle10),
            getString(R.string.subtitle11),
            getString(R.string.subtitle12),
            getString(R.string.subtitle13),
            getString(R.string.subtitle14),
            getString(R.string.subtitle15),
            getString(R.string.auto_refresh)
        )

        messages = if (prefs[SettingsKeys.REDIRECT_MODE] == "Custom URL") {
            allMessages.filter {
                !it.lowercase().contains("tfc") && !it.lowercase().contains("expando")
            }
        } else {
            allMessages.toList()
        }

        textView.animate().alpha(0f).setDuration(1000).withEndAction {
            subtitleIndex = (subtitleIndex + 1) % messages.size
            textView.text = messages[subtitleIndex]
            textView.animate().alpha(1f).setDuration(1000).start()
        }.start()
    }

    /** Handle the rocket animation */
    fun animateRocket(rocket: ImageView, firstRun: Boolean = true) {

        val screenHeight = resources.displayMetrics.heightPixels
        val screenWidth = resources.displayMetrics.widthPixels
        val orbitRadius = screenHeight * 0.55f
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        //270 = Top
        val startAngle = 30f
        val endAngle = startAngle - 360f

        val orbit = ObjectAnimator.ofFloat(startAngle, endAngle).apply {
            duration = 30000L
            repeatCount = 0
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val angle = Math.toRadians((animator.animatedValue as Float).toDouble())
                rocket.x = centerX + (orbitRadius * cos(angle)).toFloat() - rocket.width / 2f
                rocket.y = centerY + (orbitRadius * sin(angle)).toFloat() - rocket.height / 2f
                rocket.rotation = animator.animatedValue as Float
            }

            addListener(object : AnimatorListenerAdapter() {
                private var ended = false

                override fun onAnimationEnd(animation: Animator) {
                    if (ended) return
                    ended = true

                    val delay = (Math.random() * 99000 + 1000).toLong()
                    Log.d("$TAG:Rocket", "Delaying Animation - $delay ms")
                    rocket.handler?.postDelayed({
                        animateRocket(rocket, firstRun = false)
                    }, delay)
                }
            })
        }

//        rocket.setOnClickListener {
//            Toast.makeText(this, "Rocket: Not Yet Implemented", Toast.LENGTH_SHORT).show()
//        }

        if (firstRun) {
            val randy = Math.random() * 100
            var rocketImg = R.drawable.rocket
            if (randy <= 2) {
                rocketImg = R.drawable.picklerick
            } else if (randy <= 4) {
                rocketImg = R.drawable.truck_1
            } else if (randy <= 6) {
                rocketImg = R.drawable.rocket2
            } else if (randy <= 8) {
                rocketImg = R.drawable.rocket3
            }

            Log.d("$TAG:Rocket", "Randy: $randy")
            Glide.with(this).asGif().load(rocketImg).into(rocket)

            val fadeIn = ObjectAnimator.ofFloat(rocket, "alpha", 0f, 1f).apply {
                duration = 4000L
                interpolator = AccelerateInterpolator()
            }
            AnimatorSet().apply {
                play(orbit).with(fadeIn)
                start()
            }
        } else {
            rocket.alpha = 1f
            orbit.start()
        }
    }

    /** Check if TFC is accessible */
    private fun startPolling(targetURL: String, preferences: Preferences) {
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(5000)
            val timeout = 10 // Minutes
            while (true) {
                try {
                    val url = URL(targetURL)
                    Log.v("$TAG:Polling", "Trying to get $targetURL")

                    val connection = when (url.protocol) {
                        "https" -> url.openConnection() as HttpsURLConnection
                        "http" -> url.openConnection() as HttpURLConnection
                        else -> {
                            alertDialog(
                                getString(R.string.url_error),
                                getString(R.string.no_http_s),
                                R.drawable.link_error,
                                "Open Settings" to { openSettings() }
                            )
                            break
                        }
                    }

                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    val status = connection.responseCode
                    val body = if (status >= 400) {
                        connection.errorStream?.bufferedReader()?.readText() ?: ""
                    } else {
                        connection.inputStream.bufferedReader().readText()
                    }


                    connection.disconnect()
                    Log.v("$TAG:Polling", "Response: $status - ${connection.responseMessage}")

                    if (status == 404 || (status == 200 && body.contains("404"))) {
                        Log.w("$TAG:Polling", "Connected, but not healthy!")
                    } else {
                        if (preferences[SettingsKeys.DEBUG] ?: false) {
                            Log.d("$TAG:Polling", "Debug mode, not redirecting!")

                            val dur = Duration.between(bootTime, LocalDateTime.now()).seconds / 60.0
                            if (dur >= timeout) {
                                withContext(Dispatchers.Main) {
                                    binding.ivClock.visibility = View.VISIBLE
                                    binding.ivClock.setOnClickListener {
                                        alertDialog(
                                            getString(R.string.its_taking_a_long_time_to_load),
                                            getString(R.string.this_is_why_you_are_in_debug_mode),
                                            R.drawable.clock,
                                            "Open Settings" to { openSettings() }
                                        )
                                    }
                                }
                            } else {
                                Log.d(
                                    "$TAG:Polling",
                                    "Its been ${"%.2f".format(dur)} minutes. Showing clock in ${
                                        "%.2f".format(timeout - dur)
                                    }"
                                )
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                redirect(targetURL)
                            }
                            break
                        }
                    }
                } catch (e: MalformedURLException) {
                    withContext(Dispatchers.Main) {
                        alertDialog(
                            getString(R.string.url_error),
                            getString(R.string.broken_url) + "${e.message}",
                            R.drawable.link_error,
                            "Open Settings" to { openSettings() }
                        )
                    }
                    break
                } catch (e: Exception) {
                    val err = e.toString()
                    Log.e("$TAG:Polling", "Error $err")

                    val dur = Duration.between(bootTime, LocalDateTime.now()).seconds / 60.0
                    if (dur >= timeout) {
                        withContext(Dispatchers.Main) {
                            binding.ivClock.visibility = View.VISIBLE
                            binding.ivClock.setOnClickListener {
                                alertDialog(
                                    getString(R.string.its_taking_a_long_time_to_load),
                                    getString(R.string.this_is_why, e.message),
                                    R.drawable.clock
                                )
                            }
                        }
                    } else {
                        Log.d(
                            "$TAG:Polling", "Its been ${"%.2f".format(dur)} minutes. Showing clock in ${
                                "%.2f".format(timeout - dur)
                            }"
                        )
                    }
                }
                delay(5000)
            }
        }
    }

    private fun redirect(url: String) {
        val intent = Intent(this, TfcActivity::class.java)
        intent.putExtra("url", url)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    fun alertDialog(
        title: String, message: String, icon: Int, vararg buttons: Pair<String, () -> Unit>
    ) {
        val builder =
            AlertDialog.Builder(this@MainActivity).setTitle(title).setMessage(message).setIcon(icon)
                .setNeutralButton("Close") { dialog, _ -> dialog.dismiss() }

        // AlertDialog only supports 3 buttons, so take up to 3
        buttons.getOrNull(0)?.let { (label, action) ->
            builder.setPositiveButton(label) { _, _ -> action() }
        }
        buttons.getOrNull(1)?.let { (label, action) ->
            builder.setNegativeButton(label) { _, _ -> action() }
        }

        builder.show()
    }

    fun checkUpdate() {
        pollingUpdateJob?.cancel()
        pollingUpdateJob = lifecycleScope.launch(Dispatchers.IO) {
            val url = URL("https://api.github.com/repos/$gitRepo/releases/latest")

            try {
                Log.v("$TAG:Update", "Checking for updates! - $gitRepo")

                val connection = url.openConnection() as HttpsURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val status = connection.responseCode
                val body = if (status >= 400) {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                } else {
                    connection.inputStream.bufferedReader().readText()
                }
                connection.disconnect()
                val json = JSONObject(body)
                val tagName = json.getString("tag_name")

                Log.v("$TAG:Update", "Latest Tag Version: $tagName")

                withContext(Dispatchers.Main) {
                    if (isUpdateAvailable(versionName, tagName)) {
                        binding.ivCloud.visibility = View.VISIBLE
                        binding.ivCloud.setOnClickListener {
                            alertDialog(
                                getString(R.string.new_update),
                                getString(R.string.a_new_version_may_be_available_check_github_releases),
                                icon = R.drawable.github,
                                "Open GitHub" to {
                                    val url = "https://github.com/$gitRepo/releases"
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    startActivity(intent)
                                }
                            )
                        }
                        Log.v("$TAG:Update", "New Version Available!")
                    } else {
                        binding.ivCloud.visibility = View.GONE
                    }
                }


            } catch (e: Exception) {
                Log.e("$TAG:Update", "Exception - ${e.message}")
            } catch (e: Error) {
                Log.e("$TAG:Update", "Error - ${e.message}")
            }
        }
    }

    fun isUpdateAvailable(versionName: String, tagName: String): Boolean {
        val (appMajor, appMinor, appPatch) = versionName.trimStart('v').split(".")
            .map { it.toInt() }
        val (gitMajor, gitMinor, gitPatch) = tagName.trimStart('v').split(".").map { it.toInt() }
        return gitMajor > appMajor || (gitMajor == appMajor && gitMinor > appMinor) || (gitMajor == appMajor && gitMinor == appMinor && gitPatch > appPatch)
    }

    fun openSettings(){
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
    }
}