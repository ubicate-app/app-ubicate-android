package com.ubicate.ubicate.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.ubicate.ubicate.R
import com.ubicate.ubicate.repository.LocationSettingsChecker

class LocationDisabledDialogFragment : DialogFragment() {
    private lateinit var locationSettingsChecker: LocationSettingsChecker
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var checkRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationSettingsChecker = LocationSettingsChecker(requireActivity() as AppCompatActivity)

        checkRunnable = object : Runnable {
            override fun run() {
                if (locationSettingsChecker.isLocationEnabled()) {
                    dismissAllowingStateLoss()
                    (parentFragment as? MapaFragment)?.apply {
                        onLocationEnabled()
                    }
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_location_disabled, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnOpenSettings = view.findViewById<Button>(R.id.btnOpenSettings)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        btnOpenSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            handler.postDelayed({
                handler.post(checkRunnable)
            }, 1000)
        }

        btnCancel.setOnClickListener {
            handler.removeCallbacks(checkRunnable)
            requireActivity().finish()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setGravity(Gravity.CENTER)
    }

    override fun onDestroyView() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroyView()
    }
}