package com.example.firealarmsystem

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class SafetyFragment : Fragment() {

    private lateinit var rootLayout: ConstraintLayout

    // NEW: Variables for the table row
    private lateinit var tableUserName: TextView
    private lateinit var tableUserFloor: TextView
    private lateinit var tableUserStatus: TextView

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(context, "SMS Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "SMS Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_safety, container, false)

        rootLayout = view.findViewById(R.id.safety_root_layout)
        val safetySwitch = view.findViewById<SwitchMaterial>(R.id.switch_safety)
        val statusCard = view.findViewById<CardView>(R.id.card_my_status)
        val statusText = view.findViewById<TextView>(R.id.text_my_status_label)
        val statusIcon = view.findViewById<ImageView>(R.id.icon_status)

        // 1. Find the Table Views
        tableUserName = view.findViewById(R.id.table_user_name)
        tableUserFloor = view.findViewById(R.id.table_user_floor)
        tableUserStatus = view.findViewById(R.id.table_user_status)

        // 2. Load User Info from Settings to fill the table row
        val prefs = requireContext().getSharedPreferences("FireAlarmPrefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("user_name", "Me")
        val savedFloor = prefs.getString("user_room", "-")

        tableUserName.text = savedName
        tableUserFloor.text = savedFloor

        // Check permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }

        safetySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // UI: Safe (Main Box)
                statusCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                statusText.text = "I AM SAFE"
                statusText.setTextColor(Color.parseColor("#2E7D32"))
                statusIcon.setImageResource(android.R.drawable.ic_input_add)
                statusIcon.setColorFilter(Color.parseColor("#2E7D32"))

                // UI: Update Table Row to Green
                tableUserStatus.text = "SAFE"
                tableUserStatus.setTextColor(Color.parseColor("#2E7D32"))

                sendSafetySMS()

                // --- NEW LOGIC: Stop the alarm if it is currently active ---
                if (MainActivity.isFireAlarmActive) {
                    (activity as? MainActivity)?.stopEmergencyMode()
                }

            } else {
                // UI: Unsafe (Main Box)
                statusCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                statusText.text = "NOT SAFE"
                statusText.setTextColor(Color.parseColor("#C62828"))
                statusIcon.setImageResource(android.R.drawable.ic_delete)
                statusIcon.setColorFilter(Color.parseColor("#C62828"))

                // UI: Update Table Row to Red
                tableUserStatus.text = "NOT SAFE"
                tableUserStatus.setTextColor(Color.parseColor("#C62828"))
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        if (MainActivity.isFireAlarmActive) {
            rootLayout.setBackgroundColor(Color.parseColor("#FFCDD2")) // Danger Red
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#E8F5E9")) // Safe Green
        }
    }

    private fun sendSafetySMS() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val prefs = requireContext().getSharedPreferences("FireAlarmPrefs", Context.MODE_PRIVATE)
            val userName = prefs.getString("user_name", "Unknown User")
            val contacts = ArrayList<String>()
            for (i in 1..5) {
                val number = prefs.getString("contact_$i", "")
                if (!number.isNullOrEmpty()) contacts.add(number)
            }
            if (contacts.isEmpty()) {
                Toast.makeText(context, "No contacts saved! Go to Home > Manage Contacts.", Toast.LENGTH_LONG).show()
                return
            }
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requireContext().getSystemService(SmsManager::class.java) else SmsManager.getDefault()
            val message = "I have evacuated and marked myself as SAFE in the Fire Alarm App. - $userName"
            var successCount = 0
            for (number in contacts) {
                try {
                    val cleanNumber = number.replace(" ", "").replace("-", "")
                    smsManager.sendTextMessage(cleanNumber, null, message, null, null)
                    successCount++
                } catch (e: Exception) { e.printStackTrace() }
            }
            if (successCount > 0) Toast.makeText(context, "Safe status sent to $successCount contacts.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission missing! Cannot send SMS.", Toast.LENGTH_SHORT).show()
        }
    }
}