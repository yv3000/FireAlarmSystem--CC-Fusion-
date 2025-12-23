package com.example.firealarmsystem

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.Locale

class SOSFragment : Fragment(), LocationListener {

    private lateinit var locationText: TextView
    private lateinit var rootLayout: ConstraintLayout
    private var locationManager: LocationManager? = null

    // Default string in case address fetch fails
    private var currentLocationString: String = "Location not found yet"

    // TEST NUMBER (Fire Brigade Placeholder)
    private val FIRE_BRIGADE_NUMBER = "+917249667122"

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) getLocation()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sos, container, false)

        rootLayout = view.findViewById(R.id.sos_root_layout)
        val sosBtn = view.findViewById<Button>(R.id.btn_sos_call)
        locationText = view.findViewById(R.id.text_location)

        // PULSE ANIMATION if Alarm is Active
        if (MainActivity.isFireAlarmActive) {
            val scaleX = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.2f)
            val scaleY = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.2f)
            val animator = android.animation.ObjectAnimator.ofPropertyValuesHolder(sosBtn, scaleX, scaleY)
            animator.duration = 400
            animator.repeatCount = android.animation.ObjectAnimator.INFINITE
            animator.repeatMode = android.animation.ObjectAnimator.REVERSE
            animator.start()
        }

        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLocation()
        }

        sosBtn.setOnClickListener {
            // 1. CALL
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$FIRE_BRIGADE_NUMBER")
            startActivity(intent)

            // 2. SMS (Will now include the address if available)
            sendEmergencySMS()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        // Restart location updates when user returns to this tab
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        }

        if (MainActivity.isFireAlarmActive) {
            rootLayout.setBackgroundColor(Color.parseColor("#FFCDD2")) // Danger Red
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#E8F5E9")) // Safe Green
        }
    }

    // Stop updates when leaving the screen to prevent crashes
    override fun onPause() {
        super.onPause()
        locationManager?.removeUpdates(this)
    }

    private fun sendEmergencySMS() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val prefs = requireContext().getSharedPreferences("FireAlarmPrefs", Context.MODE_PRIVATE)
            val userName = prefs.getString("user_name", "Unknown User")
            val userFloor = prefs.getString("user_room", "Unknown Floor")

            val message = "EMERGENCY! Fire detected!\nName: $userName\nFloor: $userFloor\n$currentLocationString\nPlease send help!"

            val contacts = ArrayList<String>()
            for (i in 1..5) {
                val num = prefs.getString("contact_$i", "")
                if (!num.isNullOrEmpty()) contacts.add(num)
            }

            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requireContext().getSystemService(SmsManager::class.java) else SmsManager.getDefault()

            var sentCount = 0
            for (num in contacts) {
                try {
                    val cleanNum = num.replace(" ", "").replace("-", "")

                    // Use sendMultipartTextMessage for long messages
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(cleanNum, null, parts, null, null)

                    sentCount++
                } catch (e: Exception) { e.printStackTrace() }
            }

            if (sentCount > 0) Toast.makeText(context, "SOS Sent to $sentCount contacts", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "SMS Permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0f, this)
                locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0f, this)
                locationText.text = "Getting Satellite Fix..."
            }
        } catch (e: Exception) { }
    }

    override fun onLocationChanged(location: Location) {
        val currentContext = context ?: return

        // --- HARDCODED FOR TESTING ---
        // We ignore the actual 'location' input and use your specific coordinates
        val testLat = 19.213612856901946
        val testLong = 72.86476682790997

        val latStr = String.format("%.7f", testLat)
        val longStr = String.format("%.7f", testLong)

        // 1. Update Display immediately
        val coordsText = "Loc: $latStr, $longStr"
        currentLocationString = coordsText
        locationText.text = "Fetching Address...\n$coordsText"

        // 2. Fetch Address for these specific coordinates
        val geocoder = Geocoder(currentContext, Locale.getDefault())

        Thread {
            try {
                // Look up address for the TEST coordinates
                val addresses = geocoder.getFromLocation(testLat, testLong, 1)

                if (!addresses.isNullOrEmpty()) {
                    val addressObj = addresses[0]
                    val addressLine = addressObj.getAddressLine(0)

                    // 3. Update UI
                    activity?.runOnUiThread {
                        currentLocationString = "Loc: $latStr, $longStr\nAddr: $addressLine"
                        locationText.text = currentLocationString
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}