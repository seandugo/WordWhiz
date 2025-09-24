package com.example.thesis_app.ui.fragments.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appBar = view.findViewById<AppBarLayout>(R.id.appbar)
        val collapsing = view.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val nameExpanded = view.findViewById<TextView>(R.id.nameTextExpanded)
        val classExpanded = view.findViewById<TextView>(R.id.classTextExpanded)

        // ensure CollapsingToolbar won't draw its own title
        collapsing.isTitleEnabled = false

        // optional nav action
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        var isTitleShown = false
        var scrollRange = -1

        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = appBarLayout.totalScrollRange
            }

            // fully collapsed
            if (scrollRange + verticalOffset == 0) {
                toolbar.title = nameExpanded.text
                toolbar.subtitle = classExpanded.text
                toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                toolbar.setSubtitleTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                // hide expanded texts
                nameExpanded.visibility = View.GONE
                classExpanded.visibility = View.GONE
                isTitleShown = true
            } else if (isTitleShown) {
                // expanded (or in-between -> restore expanded texts and clear toolbar)
                toolbar.title = ""
                toolbar.subtitle = ""
                nameExpanded.visibility = View.VISIBLE
                classExpanded.visibility = View.VISIBLE
                isTitleShown = false
            }
        })
    }
}
