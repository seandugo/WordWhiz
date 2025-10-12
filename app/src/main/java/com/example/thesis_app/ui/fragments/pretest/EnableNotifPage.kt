package com.example.thesis_app.ui.fragments.pretest

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.example.thesis_app.PreAssessmentActivity
import com.example.thesis_app.R

class EnableNotifPage : Fragment(R.layout.notif) {
    private lateinit var nextButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButton = view.findViewById(R.id.button)

        // Back pressed handling
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                }
            })

        // Initial button update
        updateButtonText()

        // Button click
        nextButton.setOnClickListener {
            if (!areNotificationsEnabled()) {
                // Open notification settings
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
                startActivity(intent)
            } else {
                // Proceed to next fragment
                val secondPageFragment = ClassCodePage()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerView2, secondPageFragment)
                    .addToBackStack(null)
                    .commit()
                (activity as? PreAssessmentActivity)?.nextStep()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check notification status every time fragment resumes
        updateButtonText()
    }

    private fun updateButtonText() {
        if (areNotificationsEnabled()) {
            nextButton.text = "Next"
        } else {
            nextButton.text = "Enable Notifications"
        }
    }

    private fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
    }
}
