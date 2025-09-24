package com.example.thesis_app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URL
import java.util.*

class SpellingGameActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnSpeakNormal: ImageView
    private lateinit var btnSpeakSlow: ImageView
    private lateinit var btnCheck: Button
    private lateinit var btnHint: ImageView
    private lateinit var textGuess: TextView
    private lateinit var textMeaning: TextView
    private lateinit var row1: LinearLayout
    private lateinit var row2: LinearLayout
    private lateinit var btnBackSpace: ImageView
    private val revealedByHint = mutableSetOf<Int>()
    private var remainingHints = 3
    private lateinit var totalHints: TextView

    private var currentWord: String = ""
    private var currentMeaning: String = ""
    private var currentGuess: CharArray = charArrayOf()
    private lateinit var tts: TextToSpeech
    private var isAnswered = false
    private lateinit var loadingLayout: LinearLayout
    private lateinit var gameLayout: CoordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spelling_game)

        btnSpeakNormal = findViewById(R.id.btnSpeakNormal)
        btnSpeakSlow = findViewById(R.id.btnSpeakSlow)
        btnCheck = findViewById(R.id.btnCheck)
        btnHint = findViewById(R.id.btnHint)
        textGuess = findViewById(R.id.textGuess)
        textMeaning = findViewById(R.id.textMeaning)
        row1 = findViewById(R.id.row1)
        row2 = findViewById(R.id.row2)
        btnBackSpace = findViewById(R.id.backspace)
        totalHints = findViewById(R.id.totalHints)
        totalHints.text = remainingHints.toString()

        btnBackSpace.setOnClickListener {
            removeLastLetter()
        }

        tts = TextToSpeech(this, this)
        loadingLayout = findViewById(R.id.loadingLayout)
        gameLayout = findViewById(R.id.gameLayout)

        gameLayout.visibility = View.GONE
        loadingLayout.visibility = View.VISIBLE

        fetchRandomWord()


        btnSpeakNormal.setOnClickListener {
            if (currentWord.isNotEmpty()) {
                tts.setSpeechRate(1.0f) // normal speed
                tts.speak(currentWord, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        btnSpeakSlow.setOnClickListener {
            if (currentWord.isNotEmpty()) {
                tts.setSpeechRate(0.6f) // slow speed
                tts.speak(currentWord, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        btnHint.setOnClickListener {
            if (remainingHints > 0) {
                revealRandomLetter()
                remainingHints--
                totalHints.text = remainingHints.toString()

                if (remainingHints == 0) {
                    btnHint.isEnabled = false // disable if no hints left
                }
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No hints left!", Snackbar.LENGTH_SHORT).show()
            }
        }


        btnCheck.setOnClickListener {
            checkAnswer()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun fetchRandomWord() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var word: String
                var meaning: String = ""

                do {
                    val response = URL("https://random-word-api.herokuapp.com/word?number=1").readText()
                    val jsonArray = JSONArray(response)
                    word = jsonArray.getString(0)

                    // limit by length
                    if (word.length !in 3..8) continue

                    try {
                        val dictResponse = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$word").readText()
                        val jsonDef = JSONArray(dictResponse).getJSONObject(0)
                        val meanings = jsonDef.getJSONArray("meanings")
                        meaning = meanings.getJSONObject(0)
                            .getJSONArray("definitions")
                            .getJSONObject(0)
                            .getString("definition")
                    } catch (_: Exception) {
                        continue // retry if no definition found
                    }
                } while (meaning.isBlank())

                withContext(Dispatchers.Main) {
                    currentWord = word.lowercase(Locale.getDefault())
                    currentMeaning = meaning
                    currentGuess = CharArray(currentWord.length) { '_' }
                    updateWordDisplay()

                    textMeaning.text = currentMeaning
                    setupLetterButtons()

                    loadingLayout.visibility = View.GONE
                    gameLayout.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    textMeaning.text = ""
                }
            }
        }
    }

    private fun removeLastLetter() {
        // find the last filled letter index that is NOT revealed by hint
        val lastIndex = currentGuess.indices
            .lastOrNull { i -> currentGuess[i] != '_' && !revealedByHint.contains(i) }
            ?: -1

        if (lastIndex != -1) {
            val removedChar = currentGuess[lastIndex]
            currentGuess[lastIndex] = '_' // remove it
            updateWordDisplay()

            // re-enable the corresponding disabled letter button (first matching one)
            val parent: List<LinearLayout> = listOf(row1, row2)
            for (row in parent) {
                for (i in 0 until row.childCount) {
                    val btn = row.getChildAt(i) as? Button ?: continue
                    val btnText = btn.text.toString()
                    if (btnText.equals(removedChar.toString(), ignoreCase = true) && !btn.isEnabled) {
                        btn.isEnabled = true
                        return
                    }
                }
            }
        }
    }

    private fun updateWordDisplay() {
        textGuess.text = currentGuess.joinToString(" ").uppercase(Locale.getDefault())
    }

    private fun revealRandomLetter() {
        val hiddenIndexes = currentGuess.indices.filter { currentGuess[it] == '_' }
        if (hiddenIndexes.isNotEmpty()) {
            val randomIndex = hiddenIndexes.random()
            currentGuess[randomIndex] = currentWord[randomIndex]
            revealedByHint.add(randomIndex)
            updateWordDisplay()
        }
    }

    private fun setupLetterButtons() {
        row1.removeAllViews()
        row2.removeAllViews()

        val allLetters = (currentWord.toList() + ('a'..'z').shuffled().take(6)).shuffled()
        val half = (allLetters.size + 1) / 2
        addLettersToRow(allLetters.take(half), row1)
        addLettersToRow(allLetters.drop(half), row2)
    }

    private fun addLettersToRow(letters: List<Char>, row: LinearLayout) {
        row.weightSum = letters.size.toFloat()

        for (letter in letters) {
            val btn = Button(this)
            btn.text = letter.toString().uppercase()
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.setMargins(4, 4, 4, 4)
            btn.layoutParams = params

            btn.setOnClickListener {
                // fill first available underscore
                val index = currentGuess.indexOfFirst { it == '_' }
                if (index != -1) {
                    currentGuess[index] = letter
                    updateWordDisplay()
                    btn.isEnabled = false
                }
            }
            row.addView(btn)
        }
    }

    private fun checkAnswer() {
        if (isAnswered) return // prevent re-checking

        val message = if (currentGuess.joinToString("") == currentWord) {
            "✅ Correct! The word was \"$currentWord\""
        } else {
            "❌ Wrong. Correct spelling: \"$currentWord\""
        }

        // Reveal the full word
        currentGuess = currentWord.toCharArray()
        updateWordDisplay()

        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()

        // disable further input
        disableLetterButtons()
        btnBackSpace.isEnabled = false
        btnHint.isEnabled = false

        isAnswered = true
    }

    private fun disableLetterButtons() {
        val parent = listOf(row1, row2)
        for (row in parent) {
            for (i in 0 until row.childCount) {
                val btn = row.getChildAt(i) as? Button ?: continue
                btn.isEnabled = false
            }
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
