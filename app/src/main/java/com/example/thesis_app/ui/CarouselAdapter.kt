package com.example.thesis_app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.R

class CarouselAdapter(private val items: List<Pair<Int, String>>) :
    RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder>() {

    inner class CarouselViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.carouselImage)
        val text: TextView = view.findViewById(R.id.carouselText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.carousel_item, parent, false)
        return CarouselViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        val (imageRes, label) = items[position]
        holder.image.setImageResource(imageRes)
        holder.text.text = label
    }

    override fun getItemCount(): Int = items.size
}