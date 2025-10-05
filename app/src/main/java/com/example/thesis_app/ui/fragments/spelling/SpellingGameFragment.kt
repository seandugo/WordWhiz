package com.example.thesis_app.ui.fragments.spelling

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.Locale

class SpellingGameFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var btnSpeakNormal: ImageView
    private lateinit var btnSpeakSlow: ImageView
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
    private lateinit var toolBar: Toolbar
    private var cooldownTimer: CountDownTimer? = null
    private val COOLDOWN_HOURS = 3
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_spelling_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolBar = view.findViewById(R.id.toolbar)
        btnSpeakNormal = view.findViewById(R.id.btnSpeakNormal)
        btnSpeakSlow = view.findViewById(R.id.btnSpeakSlow)
        btnHint = view.findViewById(R.id.btnHint)
        textGuess = view.findViewById(R.id.textGuess)
        textMeaning = view.findViewById(R.id.textMeaning)
        row1 = view.findViewById(R.id.row1)
        row2 = view.findViewById(R.id.row2)
        btnBackSpace = view.findViewById(R.id.backspace)
        totalHints = view.findViewById(R.id.totalHints)
        totalHints.text = remainingHints.toString()

        checkCooldown()

        btnBackSpace.setOnClickListener {
            removeLastLetter()
        }

        tts = TextToSpeech(requireContext(), this)
        loadingLayout = view.findViewById(R.id.loadingLayout)
        gameLayout = view.findViewById(R.id.gameLayout)

        gameLayout.visibility = View.GONE
        loadingLayout.visibility = View.VISIBLE

        btnSpeakNormal.setOnClickListener {
            if (currentWord.isNotEmpty()) {
                tts.setSpeechRate(1.0f)
                tts.speak(currentWord, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        btnSpeakSlow.setOnClickListener {
            if (currentWord.isNotEmpty()) {
                tts.setSpeechRate(0.6f)
                tts.speak(currentWord, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        btnHint.setOnClickListener {
            if (remainingHints > 0) {
                revealRandomLetter()
                remainingHints--
                totalHints.text = remainingHints.toString()

                if (remainingHints == 0) {
                    btnHint.isEnabled = false
                }
            } else {
                Snackbar.make(requireView(), "No hints left!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun saveSpellingProgress() {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return

        val updates = mapOf(
            "lastSpellingTime" to System.currentTimeMillis(),
            "lastWord" to currentWord,
            "lastMeaning" to currentMeaning
        )
        database.child("users").child(studentId).child("spellingActivity").updateChildren(updates)
    }

    private fun checkCooldown() {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return

        database.child("users").child(studentId).child("spellingActivity")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastTime = snapshot.child("lastSpellingTime").getValue(Long::class.java) ?: 0L
                    val lastWord = snapshot.child("lastWord").getValue(String::class.java) ?: ""
                    val lastMeaning = snapshot.child("lastMeaning").getValue(String::class.java) ?: ""
                    val now = System.currentTimeMillis()
                    val diffMillis = now - lastTime
                    val cooldownMillis = COOLDOWN_HOURS * 60 * 60 * 1000L

                    if (diffMillis < cooldownMillis && lastTime > 0L) {
                        // Still on cooldown
                        val remaining = cooldownMillis - diffMillis
                        showCooldown(lastWord, lastMeaning, remaining)
                    } else {
                        // Cooldown over → new word
                        fetchRandomWord()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    fetchRandomWord()
                }
            })
    }

    private fun saveWordToSavedWords(word: String, meaning: String) {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return

        val newWordRef = database.child("users").child(studentId).child("spellingActivity").child("savedWords").push()
        val data = mapOf(
            "word" to word,
            "partOfSpeech" to word, // store the actual word
            "definitions" to listOf(mapOf("definition" to meaning)),
            "synonyms" to listOf<String>(),
            "antonyms" to listOf<String>(),
            "savedAt" to System.currentTimeMillis() // 🔹 timestamp
        )
        newWordRef.setValue(data)
    }

    private fun showCooldown(lastWord: String, lastMeaning: String, remainingMillis: Long) {
        gameLayout.visibility = View.VISIBLE
        loadingLayout.visibility = View.GONE

        // Show last word + meaning
        currentWord = lastWord
        currentMeaning = lastMeaning
        currentGuess = lastWord.toCharArray()
        updateWordDisplay()
        setGuessLayoutPadding(true)
        textMeaning.text = currentMeaning

        textGuess.setTextColor(resources.getColor(android.R.color.black, null))
        disableLetterButtons()
        btnBackSpace.visibility = View.GONE
        btnBackSpace.isEnabled = false
        btnHint.isEnabled = false

        // Show countdown
        toolBar.visibility = View.VISIBLE

        cooldownTimer?.cancel()
        cooldownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                toolBar.title = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                toolBar.visibility = View.GONE
                fetchRandomWord()
            }
        }.start()
    }

    private fun fetchRandomWord() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var word: String
                var meaning: String = ""

                // list of fallback APIs
                val apis = listOf(
                    "https://random-word-api.vercel.app/api?words=1",
                    "https://random-word-form.herokuapp.com/random/noun",
                    "https://api.api-ninjas.com/v1/randomword" // requires API key
                )

                do {
                    var response: String? = null

                    // try each API until one works
                    for (api in apis) {
                        try {
                            response = URL(api).readText()
                            break
                        } catch (e: Exception) {
                            e.printStackTrace()
                            continue
                        }
                    }

                    if (response == null) {
                        throw Exception("All word APIs failed")
                    }

                    // normalize JSON format depending on API
                    word = when {
                        response.startsWith("[") -> {
                            // API returns ["word"]
                            JSONArray(response).getString(0)
                        }
                        response.startsWith("{") && response.contains("word") -> {
                            // API returns { "word": "example" }
                            JSONObject(response).getString("word")
                        }
                        else -> response.trim('"') // fallback
                    }

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
                        continue
                    }
                } while (meaning.isBlank())

                withContext(Dispatchers.Main) {
                    currentWord = word.lowercase(Locale.getDefault())
                    currentMeaning = meaning
                    currentGuess = CharArray(currentWord.length) { '_' }
                    updateWordDisplay()
                    setGuessLayoutPadding(false)

                    textMeaning.text = currentMeaning
                    setupLetterButtons()

                    val slideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_left)
                    val slideOut = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_right)

                    loadingLayout.startAnimation(slideOut)
                    gameLayout.startAnimation(slideIn)

                    loadingLayout.visibility = View.GONE
                    gameLayout.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    textMeaning.text = "Failed to load word."
                }
            }
        }
    }

    private fun removeLastLetter() {
        val lastIndex = currentGuess.indices
            .lastOrNull { i -> currentGuess[i] != '_' && !revealedByHint.contains(i) }
            ?: -1

        if (lastIndex != -1) {
            val removedChar = currentGuess[lastIndex]
            currentGuess[lastIndex] = '_'
            updateWordDisplay()

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

            // Disable that letter’s button so it can’t be picked again
            val parent = listOf(row1, row2)
            for (row in parent) {
                for (i in 0 until row.childCount) {
                    val btn = row.getChildAt(i) as? Button ?: continue
                    if (btn.text.toString().equals(currentWord[randomIndex].toString(), ignoreCase = true)) {
                        btn.isEnabled = false
                        break
                    }
                }
            }
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
            val btn = Button(requireContext())
            btn.text = letter.toString().uppercase()
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.setMargins(4, 4, 4, 4)
            btn.layoutParams = params

            btn.setOnClickListener {
                val index = currentGuess.indexOfFirst { it == '_' }
                if (index != -1) {
                    currentGuess[index] = letter
                    updateWordDisplay()
                    btn.isEnabled = false

                    // Automatically check if all letters are filled
                    if (currentGuess.none { it == '_' }) {
                        checkAnswer()
                    }
                }
            }
            row.addView(btn)
        }
    }
    private fun checkAnswer() {
        if (isAnswered) return

        val guessWord = currentGuess.joinToString("")

        if (guessWord == currentWord) {
            setGuessLayoutPadding(true)
            // ✅ Correct
            textGuess.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            Snackbar.make(requireView(), "✅ Correct! The word was \"$currentWord\"", Snackbar.LENGTH_LONG).show()

            disableLetterButtons()
            btnBackSpace.visibility = View.GONE  // 🔹 Hide backspace
            btnHint.isEnabled = false
            isAnswered = true

            // 👉 Save word + timestamp
            saveSpellingProgress()

            saveWordToSavedWords(currentWord, currentMeaning)

            // 👉 Immediately start cooldown
            val cooldownMillis = COOLDOWN_HOURS * 60 * 60 * 1000L
            showCooldown(currentWord, currentMeaning, cooldownMillis)

        } else {
            // ❌ Wrong → blink red twice
            blinkRedTwice {
                resetWrongAnswer()
            }
            Snackbar.make(requireView(), "❌ Wrong, try again!", Snackbar.LENGTH_SHORT).show()
        }

        // 🔹 Reset text color to default (no permanent color)
        textGuess.setTextColor(resources.getColor(android.R.color.black, null))
    }

    private fun blinkRedTwice(onComplete: () -> Unit) {
        val red = resources.getColor(android.R.color.holo_red_dark, null)
        val normal = resources.getColor(android.R.color.black, null) // default text color

        val animator = ObjectAnimator.ofArgb(textGuess, "textColor", normal, red, normal, red, normal)
        animator.duration = 600 // total duration
        animator.start()

        animator.doOnEnd {
            textGuess.setTextColor(normal) // reset to default
            onComplete()
        }
    }

    private fun setGuessLayoutPadding(isComplete: Boolean) {
        val guessLayout = textGuess.parent as View
        if (isComplete) {
            // Remove left padding
            guessLayout.setPadding(
                0,
                guessLayout.paddingTop,
                guessLayout.paddingRight,
                guessLayout.paddingBottom
            )
        } else {
            // Reset to default (use dp if you want, here 16dp as example)
            val defaultPadding = (16 * resources.displayMetrics.density).toInt()
            guessLayout.setPadding(
                defaultPadding,
                guessLayout.paddingTop,
                guessLayout.paddingRight,
                guessLayout.paddingBottom
            )
        }
    }

    private fun resetWrongAnswer() {
        // Reset letters except hint-revealed ones
        for (i in currentGuess.indices) {
            if (!revealedByHint.contains(i)) {
                currentGuess[i] = '_'
            }
        }
        updateWordDisplay()

        // Re-enable all letter buttons
        val parent = listOf(row1, row2)
        for (row in parent) {
            for (i in 0 until row.childCount) {
                val btn = row.getChildAt(i) as? Button ?: continue
                btn.isEnabled = true
            }
        }

        // Keep already revealed letters disabled
        for (i in revealedByHint) {
            val revealedChar = currentWord[i].toString().uppercase()
            for (row in parent) {
                for (j in 0 until row.childCount) {
                    val btn = row.getChildAt(j) as? Button ?: continue
                    if (btn.text.toString() == revealedChar) {
                        btn.isEnabled = false
                    }
                }
            }
        }
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

    override fun onDestroyView() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroyView()
    }
}