package com.example.thesis_app.ui.fragments.spelling

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thesis_app.MeaningAdapter
import com.example.thesis_app.databinding.SavedWordsFragmentBinding
import com.example.thesis_app.models.Definition
import com.example.thesis_app.models.Meaning
import com.google.firebase.database.*
import java.util.Locale

class SavedWordsFragment : Fragment() {

    private var _binding: SavedWordsFragmentBinding? = null
    private val binding get() = _binding!!

    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var adapter: MeaningAdapter
    private lateinit var tts: TextToSpeech

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SavedWordsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tts = TextToSpeech(requireContext()) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        adapter = MeaningAdapter(emptyList(), tts)
        binding.savedWordsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.savedWordsRecycler.adapter = adapter

        loadSavedWords()
    }

    private fun loadSavedWords() {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return
        database.child("users").child(studentId).child("spellingActivity").child("savedWords")
            .orderByChild("savedAt") // 🔹 order by timestamp
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val meanings = mutableListOf<Meaning>()

                    for (wordSnap in snapshot.children) {
                        val word = wordSnap.child("word").getValue(String::class.java) ?: continue
                        val definitionsList = wordSnap.child("definitions").children.map {
                            it.child("definition").getValue(String::class.java) ?: ""
                        }
                        val synonymsList = wordSnap.child("synonyms").children.map {
                            it.getValue(String::class.java) ?: ""
                        }
                        val antonymsList = wordSnap.child("antonyms").children.map {
                            it.getValue(String::class.java) ?: ""
                        }

                        meanings.add(
                            Meaning(
                                partOfSpeech = word,
                                definitions = definitionsList.map { Definition(it) },
                                synonyms = synonymsList,
                                antonyms = antonymsList
                            )
                        )
                    }

                    meanings.reverse()

                    if (meanings.isEmpty()) {
                        binding.savedWordsRecycler.visibility = View.GONE
                        binding.emptyLayout.visibility = View.VISIBLE
                        binding.recallHeader.visibility = View.GONE // 🔹 Hide header when no words
                    } else {
                        adapter.updateNewData(meanings)
                        binding.savedWordsRecycler.visibility = View.VISIBLE
                        binding.emptyLayout.visibility = View.GONE
                        binding.recallHeader.visibility = View.VISIBLE // 🔹 Show header when words exist
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.savedWordsRecycler.visibility = View.GONE
                    binding.emptyLayout.visibility = View.VISIBLE
                }
            })
    }

    override fun onDestroyView() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroyView()
        _binding = null
    }
}
