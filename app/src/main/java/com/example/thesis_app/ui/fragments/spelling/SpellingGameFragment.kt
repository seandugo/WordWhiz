package com.example.thesis_app.ui.fragments.spelling

import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
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
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.example.thesis_app.MainActivity
import com.example.thesis_app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import kotlinx.coroutines.*
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
    private lateinit var totalHints: TextView
    private lateinit var loadingLayout: LinearLayout
    private lateinit var gameLayout: CoordinatorLayout
    private lateinit var toolBar: Toolbar
    private lateinit var passNextWord: TextView

    private var revealedByHint = mutableSetOf<Int>()
    private var remainingHints = 3
    private var currentWord = ""
    private var currentMeaning = ""
    private var currentGuess = charArrayOf()
    private var isAnswered = false
    private lateinit var tts: TextToSpeech
    private var cooldownTimer: CountDownTimer? = null
    private val COOLDOWN_HOURS = 3
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
        passNextWord = view.findViewById(R.id.passNextWord)
        loadingLayout = view.findViewById(R.id.loadingLayout)
        gameLayout = view.findViewById(R.id.gameLayout)

        tts = TextToSpeech(requireContext(), this)
        gameLayout.visibility = View.GONE
        loadingLayout.visibility = View.VISIBLE

        // Start logic
        loadOrFetchWord()

        btnBackSpace.setOnClickListener { removeLastLetter() }

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
                if (remainingHints == 0) btnHint.isEnabled = false
            } else Snackbar.make(requireView(), "No hints left!", Snackbar.LENGTH_SHORT).show()
        }

        passNextWord.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pass to Next Word?")
                .setMessage("Are you sure you want to skip this word and move to the next one? You might have to wait for the cooldown before getting another word.")
                .setPositiveButton("Yes") { dialog, _ ->
                    handleNextWord()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    // 🔹 Load existing unfinished word or fetch new one if eligible
    private fun handleNextWord() {
        // Save the skipped word to the saved words list (optional)
        saveWordToSavedWords(currentWord, currentMeaning, true)

        // Mark the current word as completed to start cooldown
        saveSpellingProgress()

        // Notify the user
        Snackbar.make(requireView(), "⏭️ Word skipped. Starting cooldown...", Snackbar.LENGTH_SHORT).show()

        // Start cooldown immediately
        val cooldownMillis = COOLDOWN_HOURS * 60 * 60 * 1000L
        showCooldown(currentWord, currentMeaning, cooldownMillis)
    }

    private fun loadOrFetchWord() {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return

        database.child("users").child(studentId).child("spellingActivity")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastTime = snapshot.child("lastSpellingTime").getValue(Long::class.java) ?: 0L
                    val lastWord = snapshot.child("lastWord").getValue(String::class.java) ?: ""
                    val lastMeaning = snapshot.child("lastMeaning").getValue(String::class.java) ?: ""
                    val isCompleted = snapshot.child("isCompleted").getValue(Boolean::class.java) ?: true
                    val now = System.currentTimeMillis()
                    val diffMillis = now - lastTime
                    val cooldownMillis = COOLDOWN_HOURS * 60 * 60 * 1000L

                    if (!isCompleted && lastWord.isNotBlank()) {
                        showUnfinishedWord(lastWord, lastMeaning)
                    } else if (diffMillis < cooldownMillis && isCompleted) {
                        showCooldown(lastWord, lastMeaning, cooldownMillis - diffMillis)
                    } else {
                        fetchRandomWord()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    fetchRandomWord()
                }
            })
    }

    private fun showUnfinishedWord(word: String, meaning: String) {
        currentWord = word
        currentMeaning = meaning
        currentGuess = CharArray(currentWord.length) { '_' }

        gameLayout.visibility = View.VISIBLE
        loadingLayout.visibility = View.GONE

        updateWordDisplay()
        textMeaning.text = meaning
        setupLetterButtons()
    }

    private fun showCooldown(lastWord: String, lastMeaning: String, remainingMillis: Long) {
        passNextWord.visibility = View.GONE
        gameLayout.visibility = View.VISIBLE
        loadingLayout.visibility = View.GONE
        currentWord = lastWord
        currentMeaning = lastMeaning
        currentGuess = lastWord.toCharArray()
        updateWordDisplay()
        setGuessLayoutPadding(true)
        textMeaning.text = currentMeaning
        disableLetterButtons()
        btnBackSpace.visibility = View.GONE
        btnHint.isEnabled = false
        toolBar.visibility = View.VISIBLE

        cooldownTimer?.cancel()
        cooldownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millis: Long) {
                val h = (millis / (1000 * 60 * 60)) % 24
                val m = (millis / (1000 * 60)) % 60
                val s = (millis / 1000) % 60
                toolBar.title = String.format("%02d:%02d:%02d", h, m, s)
            }

            override fun onFinish() {
                toolBar.visibility = View.GONE
                fetchRandomWord()
                showCooldownFinishedNotification()
            }
        }.start()
    }

    // 🔹 Fetch and save new word immediately
    private fun fetchRandomWord() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var word: String
                var meaning: String
                val apis = listOf(
                    "https://random-word-api.vercel.app/api?words=1",
                    "https://random-word-form.herokuapp.com/random/noun"
                )

                do {
                    word = ""
                    meaning = ""
                    var response: String? = null
                    for (api in apis) {
                        try {
                            response = URL(api).readText()
                            break
                        } catch (_: Exception) {}
                    }
                    if (response == null) throw Exception("No API response")

                    word = when {
                        response.startsWith("[") -> JSONArray(response).getString(0)
                        response.startsWith("{") -> JSONObject(response).getString("word")
                        else -> response.trim('"')
                    }

                    if (word.length !in 3..8) continue

                    try {
                        val dictRes = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$word").readText()
                        val json = JSONArray(dictRes).getJSONObject(0)
                        meaning = json.getJSONArray("meanings")
                            .getJSONObject(0)
                            .getJSONArray("definitions")
                            .getJSONObject(0)
                            .getString("definition")
                    } catch (_: Exception) {}
                } while (meaning.isBlank())

                withContext(Dispatchers.Main) {
                    currentWord = word.lowercase(Locale.getDefault())
                    currentMeaning = meaning
                    currentGuess = CharArray(currentWord.length) { '_' }

                    // Save immediately
                    val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
                    val studentId = prefs.getString("studentId", null)
                    if (studentId != null) {
                        val data = mapOf(
                            "lastWord" to currentWord,
                            "lastMeaning" to currentMeaning,
                            "isCompleted" to false
                        )
                        database.child("users").child(studentId).child("spellingActivity").updateChildren(data)
                    }

                    updateWordDisplay()
                    setGuessLayoutPadding(false)
                    textMeaning.text = currentMeaning
                    setupLetterButtons()

                    val slideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_left)
                    val slideOut = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_right)
                    loadingLayout.startAnimation(slideOut)
                    gameLayout.startAnimation(slideIn)
                    passNextWord.visibility = View.VISIBLE
                    loadingLayout.visibility = View.GONE
                    gameLayout.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    textMeaning.text = "Failed to load word."
                }
            }
        }
    }

    private fun showCooldownFinishedNotification() {
        val channelId = "cooldown_channel"
        val channelName = "Cooldown Notifications"
        val notificationId = 1

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 🔹 Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when cooldown is finished"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 🔹 Create intent that opens your app (MainActivity)
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openSpellingGame", true) // pass data to navigate directly
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 🔹 Build the notification
        val builder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.wordwhiz_logo) // your custom icon
            .setContentTitle("✨ Ready to Spell Again!")
            .setContentText("Your cooldown is over — you can now continue spelling.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // opens app when tapped
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }


    private fun saveSpellingProgress() {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return
        val updates = mapOf(
            "lastSpellingTime" to System.currentTimeMillis(),
            "isCompleted" to true
        )
        database.child("users").child(studentId).child("spellingActivity").updateChildren(updates)
    }

    private fun saveWordToSavedWords(word: String, meaning: String, skipped: Boolean) {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return
        val newRef = database.child("users").child(studentId).child("spellingActivity").child("savedWords").push()
        val data = mapOf(
            "word" to word,
            "partOfSpeech" to word,
            "definitions" to listOf(mapOf("definition" to meaning)),
            "synonyms" to listOf<String>(),
            "antonyms" to listOf<String>(),
            "savedAt" to System.currentTimeMillis(),
            "skipped" to skipped
        )
        newRef.setValue(data)
    }

    // --- GAME LOGIC ---

    private fun checkAnswer() {
        if (isAnswered) return
        val guessWord = currentGuess.joinToString("")
        if (guessWord == currentWord) {
            textGuess.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            Snackbar.make(requireView(), "✅ Correct! The word was \"$currentWord\"", Snackbar.LENGTH_LONG).show()
            disableLetterButtons()
            btnBackSpace.visibility = View.GONE
            btnHint.isEnabled = false
            isAnswered = true
            saveSpellingProgress()
            saveWordToSavedWords(currentWord, currentMeaning, false)
            val cooldownMillis = COOLDOWN_HOURS * 60 * 60 * 1000L
            showCooldown(currentWord, currentMeaning, cooldownMillis)
        } else {
            blinkRedTwice { resetWrongAnswer() }
            Snackbar.make(requireView(), "❌ Wrong, try again!", Snackbar.LENGTH_SHORT).show()
        }
        textGuess.setTextColor(resources.getColor(android.R.color.black, null))
    }

    private fun blinkRedTwice(onComplete: () -> Unit) {
        val red = resources.getColor(android.R.color.holo_red_dark, null)
        val normal = resources.getColor(android.R.color.black, null)
        val animator = ObjectAnimator.ofArgb(textGuess, "textColor", normal, red, normal, red, normal)
        animator.duration = 600
        animator.start()
        animator.doOnEnd { onComplete() }
    }

    private fun resetWrongAnswer() {
        for (i in currentGuess.indices) if (!revealedByHint.contains(i)) currentGuess[i] = '_'
        updateWordDisplay()
        val rows = listOf(row1, row2)
        for (r in rows) for (i in 0 until r.childCount) (r.getChildAt(i) as? Button)?.isEnabled = true
        for (i in revealedByHint) {
            val ch = currentWord[i].toString().uppercase()
            for (r in rows) for (j in 0 until r.childCount) {
                val b = r.getChildAt(j) as? Button ?: continue
                if (b.text == ch) b.isEnabled = false
            }
        }
    }

    private fun removeLastLetter() {
        val last = currentGuess.indices.lastOrNull { currentGuess[it] != '_' && !revealedByHint.contains(it) } ?: -1
        if (last != -1) {
            val removed = currentGuess[last]
            currentGuess[last] = '_'
            updateWordDisplay()
            val rows = listOf(row1, row2)
            for (r in rows) for (i in 0 until r.childCount) {
                val btn = r.getChildAt(i) as? Button ?: continue
                if (btn.text.toString().equals(removed.toString(), ignoreCase = true) && !btn.isEnabled) {
                    btn.isEnabled = true
                    return
                }
            }
        }
    }

    private fun updateWordDisplay() {
        textGuess.text = currentGuess.joinToString(" ").uppercase(Locale.getDefault())
    }

    private fun revealRandomLetter() {
        val hidden = currentGuess.indices.filter { currentGuess[it] == '_' }
        if (hidden.isNotEmpty()) {
            val i = hidden.random()
            currentGuess[i] = currentWord[i]
            revealedByHint.add(i)
            updateWordDisplay()
            val rows = listOf(row1, row2)
            for (r in rows) for (j in 0 until r.childCount) {
                val btn = r.getChildAt(j) as? Button ?: continue
                if (btn.text.toString().equals(currentWord[i].toString(), ignoreCase = true)) {
                    btn.isEnabled = false
                    break
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
        for (ch in letters) {
            val btn = Button(requireContext())
            btn.text = ch.toString().uppercase()
            val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            p.setMargins(4, 4, 4, 4)
            btn.layoutParams = p
            btn.setOnClickListener {
                val idx = currentGuess.indexOfFirst { it == '_' }
                if (idx != -1) {
                    currentGuess[idx] = ch
                    updateWordDisplay()
                    btn.isEnabled = false
                    if (currentGuess.none { it == '_' }) checkAnswer()
                }
            }
            row.addView(btn)
        }
    }

    private fun disableLetterButtons() {
        val rows = listOf(row1, row2)
        for (r in rows) for (i in 0 until r.childCount) (r.getChildAt(i) as? Button)?.isEnabled = false
    }

    private fun setGuessLayoutPadding(done: Boolean) {
        val layout = textGuess.parent as View
        val dp = (16 * resources.displayMetrics.density).toInt()
        if (done) layout.setPadding(0, layout.paddingTop, layout.paddingRight, layout.paddingBottom)
        else layout.setPadding(dp, layout.paddingTop, layout.paddingRight, layout.paddingBottom)
    }

    override fun onDestroyView() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        cooldownTimer?.cancel()
        super.onDestroyView()
    }
}
