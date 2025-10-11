package com.example.thesis_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.DefinitionModel
import org.json.JSONObject
import java.io.InputStream
import android.widget.TextView

class LectureReviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lecture_review)

        val recyclerView = findViewById<RecyclerView>(R.id.lectureRecycler)
        val headerTitle = findViewById<TextView>(R.id.nameTextExpanded)
        val headerSubtitle = findViewById<TextView>(R.id.lecture)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Go back to previous screen
        }

        // --- Get intent extras ---
        val quizPart = intent.getStringExtra("quiz_part") ?: ""
        val quizOrder = intent.getStringExtra("quiz_order")?.toIntOrNull() ?: 0

        // --- Set header text ---
        headerTitle.text = quizPart
        headerSubtitle.text = "Lecture ${quizOrder}"

        // --- Choose JSON based on order ---
        val resourceId = when (quizOrder - 1) {
            0 -> R.raw.analogies
            1 -> R.raw.genre_viewing
            2 -> R.raw.active_passive
            3 -> R.raw.simple_past
            4 -> R.raw.directed_report
            5 -> R.raw.figurative
            6 -> R.raw.informative_essay
            7 -> R.raw.nouns_adverbs
            8 -> R.raw.verbs_adjectives
            9 -> R.raw.comparison_contrast
            10 -> R.raw.cohesive_devices
            11 -> R.raw.modal_verbs
            12 -> R.raw.modal_verbs_2
            13 -> R.raw.comm_style
            14 -> R.raw.text_connections
            15 -> R.raw.understanding_unc_val
            16 -> R.raw.authors_purpose
            17 -> R.raw.analysis_comparison
            else -> R.raw.analogies // fallback
        }

        // --- Load and parse JSON ---
        val jsonString = loadJSONFromRaw(resourceId)
        val definitions = parseJSON(jsonString)

        recyclerView.adapter = DefinitionAdapter(definitions)
    }

    private fun loadJSONFromRaw(resourceId: Int): String {
        val inputStream: InputStream = resources.openRawResource(resourceId)
        return inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseJSON(jsonString: String): List<DefinitionModel> {
        val list = mutableListOf<DefinitionModel>()
        val jsonObject = JSONObject(jsonString)
        val definitionsArray = jsonObject.getJSONArray("definitions")

        for (i in 0 until definitionsArray.length()) {
            val obj = definitionsArray.getJSONObject(i)
            list.add(
                DefinitionModel(
                    number = obj.getInt("number"),
                    title = obj.getString("title"),
                    emoji = obj.getString("emoji"),
                    definition = obj.getString("definition"),
                    example = obj.getString("example"),
                    explanation = obj.optString("explanation", "")
                )
            )
        }
        return list
    }
}
