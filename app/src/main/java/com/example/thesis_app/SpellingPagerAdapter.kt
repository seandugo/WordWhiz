package com.example.thesis_app.ui.fragments.student

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.thesis_app.ui.fragments.spelling.SavedWordsFragment
import com.example.thesis_app.ui.fragments.spelling.SpellingFragment
import com.example.thesis_app.ui.fragments.spelling.SpellingGameFragment

class SpellingPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SpellingGameFragment()     // your existing spelling fragment
            1 -> SavedWordsFragment()   // your existing saved words fragment
            else -> SpellingFragment()
        }
    }
}
