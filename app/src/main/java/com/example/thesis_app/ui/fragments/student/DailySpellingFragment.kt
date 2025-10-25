package com.example.thesis_app.ui.fragments.student

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.thesis_app.R
import com.example.thesis_app.ui.fragments.nointernet.NoInternetFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DailySpellingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // ✅ Step 1: Check internet before inflating main layout
        if (!isInternetAvailable()) {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, NoInternetFragment()) // Replace with NoInternetFragment
                .commit()
            return null
        }

        // ✅ Step 2: Inflate layout and set up tabs
        val view = inflater.inflate(R.layout.daily_spelling, container, false)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        val adapter = SpellingPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Spelling"
                1 -> tab.text = "Words"
            }
        }.attach()

        return view
    }

    // ✅ Step 3: Internet checking helper
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
