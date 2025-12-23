package com.example.firealarmsystem

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        var isFireAlarmActive = false
        var hasUserMarkedSafe = false
    }

    private lateinit var bottomNav: BottomNavigationView
    private var sosAnimator: ObjectAnimator? = null

    // Hardware
    private var vibrator: Vibrator? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var audioManager: AudioManager? = null
    private var toneGen: ToneGenerator? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isFlashOn = false
    private val LOOP_DELAY_MS: Long = 400

    private val blinkAndBeepRunnable = object : Runnable {
        override fun run() {
            if (isFireAlarmActive) {
                toggleFlashlight()
                if (isFlashOn) {
                    toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                }
                handler.postDelayed(this, LOOP_DELAY_MS)
            }
            else {
                turnFlashOff()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try { cameraId = cameraManager?.cameraIdList?.get(0) } catch (e: Exception) { }

        bottomNav = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_sos -> { loadFragment(SOSFragment()); true }
                R.id.nav_map -> { loadFragment(MapFragment()); true }
                R.id.nav_safe -> { loadFragment(SafetyFragment()); true }
                else -> false
            }
        }
    }

    // --- PUBLIC FUNCTIONS ---

    fun startEmergencyMode() {
        if (isFireAlarmActive) return
        isFireAlarmActive = true
        hasUserMarkedSafe = false

        val badge = bottomNav.getOrCreateBadge(R.id.nav_sos)
        badge.backgroundColor = Color.RED
        badge.number = 1
        badge.isVisible = true

        try {
            val sosView = bottomNav.findViewById<View>(R.id.nav_sos)
            if (sosView != null) {
                val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.5f)
                val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.5f)
                sosAnimator = ObjectAnimator.ofPropertyValuesHolder(sosView, scaleX, scaleY)
                sosAnimator?.duration = 500
                sosAnimator?.repeatCount = Animation.INFINITE
                sosAnimator?.repeatMode = ObjectAnimator.REVERSE
                sosAnimator?.start()
            }
        } catch (e: Exception) { e.printStackTrace() }

        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 100
        audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        try { toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100) } catch (e: Exception) { }

        startSyncedVibration()
        handler.post(blinkAndBeepRunnable)
    }

    fun stopEmergencyMode() {
        isFireAlarmActive = false

        val badge = bottomNav.getBadge(R.id.nav_sos)
        badge?.isVisible = false
        badge?.clearNumber()
        sosAnimator?.cancel()
        val sosView = bottomNav.findViewById<View>(R.id.nav_sos)
        sosView?.scaleX = 1.0f
        sosView?.scaleY = 1.0f

        vibrator?.cancel()
        turnFlashOff()
        toneGen?.release()
        toneGen = null
        handler.removeCallbacks(blinkAndBeepRunnable)
    }

    private fun startSyncedVibration() {
        if (vibrator != null && vibrator!!.hasVibrator()) {
            val timings = longArrayOf(0, LOOP_DELAY_MS, LOOP_DELAY_MS)
            val amplitudes = intArrayOf(0, 255, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                vibrator?.vibrate(effect)
            }
            else {
                vibrator?.vibrate(timings, 0)
            }
        }
    }

    private fun toggleFlashlight() { try { if (cameraId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { isFlashOn = !isFlashOn; cameraManager?.setTorchMode(cameraId!!, isFlashOn) } } catch (e: Exception) {} }
    private fun turnFlashOff() { try { if (cameraId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { cameraManager?.setTorchMode(cameraId!!, false); isFlashOn = false } } catch (e: Exception) {} }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }
}