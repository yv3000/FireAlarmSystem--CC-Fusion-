package com.example.firealarmsystem

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private val CHANNEL_ID = "fire_alarm_channel"
    private val NOTIFICATION_ID = 1

    // UI Variables
    private lateinit var rootLayout: ScrollView
    private lateinit var cardStatus: CardView
    private lateinit var textStatus: TextView
    private lateinit var textDetails: TextView
    private lateinit var btnTest: Button // Restored this

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Initialize Views
        rootLayout = view.findViewById(R.id.home_root_layout)
        cardStatus = view.findViewById(R.id.card_status)
        textStatus = view.findViewById(R.id.text_status)
        textDetails = view.findViewById(R.id.text_details)
        btnTest = view.findViewById(R.id.btn_test_alarm) // Find the camouflaged button

        val etName = view.findViewById<EditText>(R.id.et_user_name)
        val etRoom = view.findViewById<EditText>(R.id.et_room_no)
        val btnSaveProfile = view.findViewById<Button>(R.id.btn_save_profile)
        val btnManageContacts = view.findViewById<Button>(R.id.btn_manage_contacts_home)

        // Load Profile
        val prefs = requireContext().getSharedPreferences("FireAlarmPrefs", Context.MODE_PRIVATE)
        etName.setText(prefs.getString("user_name", ""))
        etRoom.setText(prefs.getString("user_room", ""))

        btnSaveProfile.setOnClickListener {
            val editor = prefs.edit()
            editor.putString("user_name", etName.text.toString())
            editor.putString("user_room", etRoom.text.toString())
            editor.apply()
            Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
        }

        btnManageContacts.setOnClickListener { showContactsDialog() }

        // Restored Click Listener
        btnTest.setOnClickListener {
            if (!MainActivity.isFireAlarmActive) {
                startFireAlarm()
            } else {
                stopFireAlarm()
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        if (MainActivity.isFireAlarmActive) {
            setUiToDanger()
        } else {
            setUiToSafe()
        }
    }

    private fun startFireAlarm() {
        (activity as? MainActivity)?.startEmergencyMode()
        setUiToDanger()
        sendNotification()
        sendAlertSmsToContacts()
    }

    private fun stopFireAlarm() {
        (activity as? MainActivity)?.stopEmergencyMode()
        setUiToSafe()
        with(NotificationManagerCompat.from(requireContext())) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                cancel(NOTIFICATION_ID)
            }
        }
    }

    private fun setUiToDanger() {
        rootLayout.setBackgroundColor(Color.parseColor("#FFCDD2"))
        cardStatus.setCardBackgroundColor(Color.RED)
        textStatus.text = "FIRE!"
        textDetails.text = "Emergency! Evacuate immediately!"
        // Button remains invisible but clickable
    }

    private fun setUiToSafe() {
        rootLayout.setBackgroundColor(Color.parseColor("#E8F5E9"))
        cardStatus.setCardBackgroundColor(Color.parseColor("#4CAF50"))
        textStatus.text = "SAFE"
        textDetails.text = "System is active. No threats."
    }

    private fun sendAlertSmsToContacts() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val prefs = requireContext().getSharedPreferences("FireAlarmPrefs", Context.MODE_PRIVATE)
            val userName = prefs.getString("user_name", "A User")
            val roomNo = prefs.getString("user_room", "Unknown Room")
            val message = "FIRE ALERT! Sensors detected fire at $userName's location (Room: $roomNo). Please send help!"
            val contacts = ArrayList<String>()
            for (i in 1..5) {
                val num = prefs.getString("contact_$i", "")
                if (!num.isNullOrEmpty()) contacts.add(num)
            }
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requireContext().getSystemService(SmsManager::class.java) else SmsManager.getDefault()
            for (num in contacts) {
                try {
                    val cleanNum = num.replace(" ", "").replace("-", "")
                    smsManager.sendTextMessage(cleanNum, null, message, null, null)
                } catch (e: Exception) { }
            }
        }
    }

    private fun showContactsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_contacts, null)
        val et1 = dialogView.findViewById<EditText>(R.id.et_contact_1)
        val et2 = dialogView.findViewById<EditText>(R.id.et_contact_2)
        val et3 = dialogView.findViewById<EditText>(R.id.et_contact_3)
        val et4 = dialogView.findViewById<EditText>(R.id.et_contact_4)
        val et5 = dialogView.findViewById<EditText>(R.id.et_contact_5)
        val prefs = requireContext().getSharedPreferences("FireAlarmPrefs", Context.MODE_PRIVATE)
        et1.setText(prefs.getString("contact_1", ""))
        et2.setText(prefs.getString("contact_2", ""))
        et3.setText(prefs.getString("contact_3", ""))
        et4.setText(prefs.getString("contact_4", ""))
        et5.setText(prefs.getString("contact_5", ""))
        android.app.AlertDialog.Builder(requireContext()).setView(dialogView).setTitle("Manage Contacts").setPositiveButton("Save") { _, _ ->
            val editor = prefs.edit()
            editor.putString("contact_1", et1.text.toString().trim()); editor.putString("contact_2", et2.text.toString().trim())
            editor.putString("contact_3", et3.text.toString().trim()); editor.putString("contact_4", et4.text.toString().trim())
            editor.putString("contact_5", et5.text.toString().trim()); editor.apply()
            Toast.makeText(context, "Contacts Saved!", Toast.LENGTH_SHORT).show()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Fire Alarm Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "High priority alerts"; setSound(null, null); enableVibration(false)
            }
            (requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(requireContext(), MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("FIRE DETECTED!").setContentText("Emergency! Evacuate immediately.").setPriority(NotificationCompat.PRIORITY_MAX).setContentIntent(pendingIntent).setAutoCancel(true).setColor(Color.RED).setOngoing(true).setOnlyAlertOnce(true)
        with(NotificationManagerCompat.from(requireContext())) { notify(NOTIFICATION_ID, builder.build()) }
    }
}