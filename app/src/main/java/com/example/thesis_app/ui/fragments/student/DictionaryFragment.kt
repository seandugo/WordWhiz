package com.example.thesis_app.ui.fragments.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import com.example.thesis_app.MeaningAdapter
import com.example.thesis_app.R
import com.example.thesis_app.models.RetrofitInstance
import com.example.thesis_app.models.WordResult
import android.widget.ImageView

class DictionaryFragment : Fragment() {

    private lateinit var searchBtn: Button
    private lateinit var searchInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var wordTextview: TextView
    private lateinit var phoneticTextview: TextView
    private lateinit var meaningRecyclerView: RecyclerView
    private lateinit var statusTextview: TextView
    private lateinit var searchIcon: ImageView
    private lateinit var adapter: MeaningAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dictionary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views...
        searchBtn = view.findViewById(R.id.search_btn)
        searchInput = view.findViewById(R.id.search_input)
        searchIcon = view.findViewById(R.id.search)
        progressBar = view.findViewById(R.id.progress_bar)
        wordTextview = view.findViewById(R.id.word_textview)
        phoneticTextview = view.findViewById(R.id.phonetic_textview)
        meaningRecyclerView = view.findViewById(R.id.meaning_recycler_view)
        statusTextview = view.findViewById(R.id.status_textview)

        adapter = MeaningAdapter(emptyList())
        meaningRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        meaningRecyclerView.adapter = adapter

        // 👇 Show default message at first load
        searchIcon.visibility = View.VISIBLE
        statusTextview.text = "Search for a word to see results"
        statusTextview.visibility = View.VISIBLE

        searchBtn.setOnClickListener {
            val word = searchInput.text.toString().trim()
            if (word.isEmpty()) {
                searchIcon.visibility = View.VISIBLE
                statusTextview.text = "Please enter a word to search"
                statusTextview.visibility = View.VISIBLE
            } else {
                getMeaning(word)
            }
        }
    }

    private fun getMeaning(word: String) {
        setInProgress(true)
        GlobalScope.launch {
            try {
                val response = RetrofitInstance.dictionaryApi.getMeaning(word)

                activity?.runOnUiThread {
                    setInProgress(false)

                    val result = response.body()
                    if (result.isNullOrEmpty()) {
                        // ✅ No results found
                        wordTextview.text = word
                        phoneticTextview.text = ""
                        adapter.updateNewData(emptyList())
                        searchIcon.visibility = View.VISIBLE
                        statusTextview.text = "0 search results for \"$word\""
                        statusTextview.visibility = View.VISIBLE
                    } else {
                        // ✅ At least one result found
                        setUI(result.first())
                        searchIcon.visibility = View.GONE
                        statusTextview.text = ""   // clear message
                        statusTextview.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    setInProgress(false)
                    statusTextview.text = "Something went wrong"
                    statusTextview.visibility = View.VISIBLE
                }
            }
        }
    }


    private fun setUI(response: WordResult) {
        wordTextview.text = response.word
        phoneticTextview.text = response.phonetic
        adapter.updateNewData(response.meanings)
    }

    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            searchBtn.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
        } else {
            searchBtn.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
        }
    }
}
