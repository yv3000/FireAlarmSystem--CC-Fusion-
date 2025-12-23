package com.example.firealarmsystem

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView

class MapFragment : Fragment() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var mapImage: ImageView

    // Highlights
    private lateinit var highlightTop: View
    private lateinit var highlightSide: View
    private lateinit var indicatorFire: CardView
    private lateinit var indicatorUser: CardView

    // Animation holders
    private var animTop: ObjectAnimator? = null
    private var animSide: ObjectAnimator? = null
    private var animFire: ObjectAnimator? = null

    // Track current floor
    private var currentFloorName: String = "Select Floor"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        rootLayout = view.findViewById(R.id.map_root_layout)
        val spinner: Spinner = view.findViewById(R.id.spinner_floor)
        mapImage = view.findViewById(R.id.image_floor_plan)

        // Find views
        highlightTop = view.findViewById(R.id.highlight_stairs_top)
        highlightSide = view.findViewById(R.id.highlight_stairs_side)
        indicatorFire = view.findViewById(R.id.indicator_fire)
        indicatorUser = view.findViewById(R.id.indicator_user)

        val floors = arrayOf("Select Floor", "Floor 1", "Floor 2", "Floor 3")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, floors)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentFloorName = floors[position]

                // Reset all animations momentarily
                stopPulsing()

                when (currentFloorName) {
                    "Floor 1" -> mapImage.setImageResource(R.drawable.floor_1)
                    "Floor 2" -> mapImage.setImageResource(R.drawable.floor_2)
                    "Floor 3" -> mapImage.setImageResource(R.drawable.floor_3)
                    else -> mapImage.setImageDrawable(null)
                }

                // Update visibility based on which floor is active
                checkAlarmAndHighlight()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        if (MainActivity.isFireAlarmActive) {
            rootLayout.setBackgroundColor(Color.parseColor("#FFCDD2")) // Red Background
            checkAlarmAndHighlight()
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"))
            stopPulsing()
        }
    }

    override fun onPause() {
        super.onPause()
        stopPulsing()
    }

    private fun checkAlarmAndHighlight() {
        if (mapImage.drawable == null) {
            // No floor selected, hide everything
            stopPulsing()
            indicatorUser.visibility = View.INVISIBLE
            return
        }

        // --- 1. USER LOCATION LOGIC ---
        // Requirement: User is ONLY on Floor 2
        if (currentFloorName == "Floor 1") {
            indicatorUser.visibility = View.VISIBLE

            // Optional: You can explicitly set position programmatically if needed
            // setViewPosition(indicatorUser, verticalBias = 0.85f, horizontalBias = 0.5f)
        } else {
            indicatorUser.visibility = View.INVISIBLE
        }

        // --- 2. FIRE ALARM LOGIC ---
        if (MainActivity.isFireAlarmActive) {
            // Show stair highlights on ALL floors (since they are exits)
            startPulsing(highlightTop)
            startPulsing(highlightSide)

            // Requirement: Fire is ONLY on Floor 1
            if (currentFloorName == "Floor 1") {
                startPulsing(indicatorFire)
                // Optional: setViewPosition(indicatorFire, 0.25f, 0.30f)
            } else {
                indicatorFire.visibility = View.INVISIBLE
                animFire?.cancel() // Stop animating hidden view
            }
        } else {
            // Alarm OFF: Hide hazards
            highlightTop.visibility = View.INVISIBLE
            highlightSide.visibility = View.INVISIBLE
            indicatorFire.visibility = View.INVISIBLE
            animFire?.cancel()
        }
    }

    private fun startPulsing(target: View) {
        target.visibility = View.VISIBLE

        if (target == highlightTop && animTop?.isRunning == true) return
        if (target == highlightSide && animSide?.isRunning == true) return
        if (target == indicatorFire && animFire?.isRunning == true) return

        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
        val animator = ObjectAnimator.ofPropertyValuesHolder(target, alpha).apply {
            duration = 500
            repeatCount = Animation.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }

        if (target == highlightTop) animTop = animator
        if (target == highlightSide) animSide = animator
        if (target == indicatorFire) animFire = animator
    }

    private fun stopPulsing() {
        highlightTop.visibility = View.INVISIBLE
        highlightSide.visibility = View.INVISIBLE
        indicatorFire.visibility = View.INVISIBLE

        // Note: User indicator is NOT an alarm animation, so we don't hide it here.
        // It is handled in checkAlarmAndHighlight()

        animTop?.cancel()
        animSide?.cancel()
        animFire?.cancel()

        animTop = null
        animSide = null
        animFire = null
    }

    // Helper function if you ever want to change X/Y programmatically
    /*
    private fun setViewPosition(view: View, verticalBias: Float, horizontalBias: Float) {
        val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = verticalBias
        layoutParams.horizontalBias = horizontalBias
        view.layoutParams = layoutParams
    }
    */
}