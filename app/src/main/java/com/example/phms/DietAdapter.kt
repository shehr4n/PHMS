package com.example.phms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DietAdapter(
    private val dietEntries: List<DietEntry>,
    private val onItemClick: (DietEntry) -> Unit
) : RecyclerView.Adapter<DietAdapter.DietViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    inner class DietViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val caloriesTextView: TextView = itemView.findViewById(R.id.caloriesTextView)
        val weightTextView: TextView = itemView.findViewById(R.id.weightTextView)
        val foodItemsCountTextView: TextView = itemView.findViewById(R.id.foodItemsCountTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(dietEntries[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DietViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diet_entry, parent, false)
        return DietViewHolder(view)
    }

    override fun onBindViewHolder(holder: DietViewHolder, position: Int) {
        val ctx = holder.itemView.context
        val entry = dietEntries[position]

        holder.dateTextView.text = dateFormat.format(entry.date)

        // use placeholder string
        holder.caloriesTextView.text =
            ctx.getString(R.string.calories_format, entry.totalCalories)

        // weight or “not recorded”
        holder.weightTextView.text = if (entry.weight != null) {
            ctx.getString(R.string.weight_format, entry.weight)
        } else {
            ctx.getString(R.string.weight_not_recorded)
        }

        // plural for food‑items count
        val count = entry.foodItems.size
        holder.foodItemsCountTextView.text =
            ctx.resources.getQuantityString(R.plurals.food_items_count, count, count)
    }


    override fun getItemCount() = dietEntries.size
} 