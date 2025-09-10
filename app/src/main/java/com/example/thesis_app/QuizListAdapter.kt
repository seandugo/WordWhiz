package com.example.thesis_app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.QuizModel

class QuizListAdapter(private val quizModelList: List<QuizModel>) :
    RecyclerView.Adapter<QuizListAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quizTitleText: TextView = itemView.findViewById(R.id.quiz_title_text)
        private val quizSubtitleText: TextView = itemView.findViewById(R.id.quiz_subtitle_text)
        private val quizTimeText: TextView = itemView.findViewById(R.id.quiz_time_text)

        fun bind(model: QuizModel) {
            quizTitleText.text = model.title
            quizSubtitleText.text = model.subtitle
            quizTimeText.text = "${model.time} min"

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, QuizActivity::class.java)
                QuizActivity.questionModelList = model.questionList
                QuizActivity.time = model.time
                itemView.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.quiz_item_recycler_row, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int = quizModelList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(quizModelList[position])
    }
}
