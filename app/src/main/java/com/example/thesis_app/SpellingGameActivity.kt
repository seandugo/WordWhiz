package com.example.thesis_app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.thesis_app.models.RetrofitInstance
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URL
import java.util.*

class SpellingGameActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnSpeak: Button
    private lateinit var btnCheck: Button
    private lateinit var textGuess: TextView
    private lateinit var textResult: TextView
    private lateinit var textMeaning: TextView
    private lateinit var tts: TextToSpeech
    private lateinit var row1: LinearLayout
    private lateinit var row2: LinearLayout

    private var currentWord: String = ""
    private var currentMeaning: String = ""
    private var currentGuess: StringBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spelling_game)

        btnSpeak = findViewById(R.id.btnSpeak)
        btnCheck = findViewById(R.id.btnCheck)
        textGuess = findViewById(R.id.textGuess)
        textResult = findViewById(R.id.textResult)
        textMeaning = findViewById(R.id.textMeaning)
        row1 = findViewById(R.id.row1)
        row2 = findViewById(R.id.row2)

        tts = TextToSpeech(this, this)

        fetchRandomWord()

        btnSpeak.setOnClickListener {
            if (currentWord.isNotEmpty()) {
                tts.speak(currentWord, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        btnCheck.setOnClickListener {
            checkAnswer()
        }
    }

    private fun fetchRandomWord() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Get random word
                val response = URL("https://random-word-api.herokuapp.com/word?number=1").readText()
                val jsonArray = JSONArray(response)
                val word = jsonArray.getString(0)

                // 2. Get meaning
                var meaning = "No definition found."
                try {
                    val dictResponse = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$word").readText()
                    val jsonDef = JSONArray(dictResponse).getJSONObject(0)
                    val meanings = jsonDef.getJSONArray("meanings")
                    meaning = meanings.getJSONObject(0)
                        .getJSONArray("definitions")
                        .getJSONObject(0)
                        .getString("definition")
                } catch (_: Exception) {}

                // 3. Update UI
                withContext(Dispatchers.Main) {
                    currentWord = word.lowercase(Locale.getDefault())
                    currentMeaning = meaning
                    currentGuess.clear()
                    textGuess.text = ""
                    textResult.text = "Spell the word!"
                    textMeaning.text = ""
                    setupLetterButtons()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    textResult.text = "Error fetching word"
                }
            }
        }
    }

    private fun setupLetterButtons() {
        row1.removeAllViews()
        row2.removeAllViews()

        // Add extra random letters
        val allLetters = (currentWord.toList() + ('a'..'z').shuffled().take(6)).shuffled()

        // Split into two rows (max 12 per row like 4 Pics 1 Word)
        val half = (allLetters.size + 1) / 2
        val firstRow = allLetters.take(half)
        val secondRow = allLetters.drop(half)

        addLettersToRow(firstRow, row1)
        addLettersToRow(secondRow, row2)
    }

    private fun addLettersToRow(letters: List<Char>, row: LinearLayout) {
        row.weightSum = letters.size.toFloat()

        for (letter in letters) {
            val btn = Button(this)
            btn.text = letter.toString().uppercase()

            val params = LinearLayout.LayoutParams(
                0,  // width 0 so weight distributes it
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f  // each button takes equal share
            ).apply {
                setMargins(4, 4, 4, 4)
            }
            btn.layoutParams = params

            btn.setOnClickListener {
                currentGuess.append(letter)
                textGuess.text = currentGuess.toString()
                btn.isEnabled = false
            }

            row.addView(btn)
        }
    }

    private fun checkAnswer() {
        if (currentGuess.toString().equals(currentWord, ignoreCase = true)) {
            textResult.text = "✅ Correct! The word was \"$currentWord\""
        } else {
            textResult.text = "❌ Wrong. Correct spelling: \"$currentWord\""
        }
        textMeaning.text = "📖 Meaning: $currentMeaning"
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
