package com.example.phms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SearchResultsAdapter(
    private val results: List<SearchResult>,
    private val onItemClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.SearchViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val typeTextView: TextView = itemView.findViewById(R.id.typeTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val detailsTextView: TextView = itemView.findViewById(R.id.detailsTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(results[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val result = results[position]
        
        holder.titleTextView.text = result.title
        holder.typeTextView.text = result.type
        holder.dateTextView.text = dateFormat.format(result.date)
        holder.detailsTextView.text = result.details
    }

    override fun getItemCount() = results.size
} 