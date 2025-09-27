package com.example.thesis_app

import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.databinding.MeaningRecyclerRowBinding
import com.example.thesis_app.models.Meaning
class MeaningAdapter(
    private var meaningList: List<Meaning>,
    private val tts: TextToSpeech
) : RecyclerView.Adapter<MeaningAdapter.MeaningViewHolder>() {

    inner class MeaningViewHolder(private val binding: MeaningRecyclerRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(meaning: Meaning) {
            // Display the word
            binding.partOfSpeechTextview.text = meaning.partOfSpeech.uppercase()

            // Definitions
            binding.definitionsTextview.text = meaning.definitions.joinToString("\n\n") {
                val currentIndex = meaning.definitions.indexOf(it)
                "${currentIndex + 1}. ${it.definition}"
            }

            // Synonyms
            if (meaning.synonyms.isEmpty()) {
                binding.synonymsTitleTextview.visibility = View.GONE
                binding.synonymsTextview.visibility = View.GONE
            } else {
                binding.synonymsTitleTextview.visibility = View.VISIBLE
                binding.synonymsTextview.visibility = View.VISIBLE
                binding.synonymsTextview.text = meaning.synonyms.joinToString(", ")
            }

            // Antonyms
            if (meaning.antonyms.isEmpty()) {
                binding.antonymsTitleTextview.visibility = View.GONE
                binding.antonymsTextview.visibility = View.GONE
            } else {
                binding.antonymsTitleTextview.visibility = View.VISIBLE
                binding.antonymsTextview.visibility = View.VISIBLE
                binding.antonymsTextview.text = meaning.antonyms.joinToString(", ")
            }

            // 🔊 TTS Button
            binding.tts.setOnClickListener {
                if (meaning.partOfSpeech.isNotEmpty()) {
                    tts.setSpeechRate(1.0f)
                    tts.speak(meaning.partOfSpeech, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    fun updateNewData(newMeaningList: List<Meaning>) {
        meaningList = newMeaningList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeaningViewHolder {
        val binding = MeaningRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MeaningViewHolder(binding)
    }

    override fun getItemCount(): Int = meaningList.size

    override fun onBindViewHolder(holder: MeaningViewHolder, position: Int) {
        holder.bind(meaningList[position])
    }
}