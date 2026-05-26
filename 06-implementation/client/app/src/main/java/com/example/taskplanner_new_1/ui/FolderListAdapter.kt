package com.example.taskplanner_new_1.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskplanner_new_1.R
import com.example.taskplanner_new_1.data.Folder

class FolderListAdapter(
    private val onClick:       (Folder) -> Unit,
    private val onDeleteClick: (Folder) -> Unit
) : ListAdapter<Folder, FolderListAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val stripe:     View        = view.findViewById(R.id.folder_color_stripe)
        val tvName:     TextView    = view.findViewById(R.id.tv_folder_name)
        val tvCount:    TextView    = view.findViewById(R.id.tv_folder_task_count)
        val tvPriority: TextView    = view.findViewById(R.id.tv_folder_priority)
        val btnDelete:  ImageButton = view.findViewById(R.id.btn_delete_folder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val folder = getItem(position)
        holder.tvName.text  = folder.name
        holder.tvCount.text = taskCountLabel(folder.taskCount)

        val hex   = Folder.FOLDER_COLORS.getOrElse(folder.colorIndex) { "#2196F3" }
        val color = Color.parseColor(hex)

        // Left colour stripe
        holder.stripe.setBackgroundColor(color)

        // Priority badge — pill shape with folder colour
        holder.tvPriority.text = Folder.COLOR_NAMES.getOrElse(folder.colorIndex) { "" }
        val pill = GradientDrawable().apply {
            shape         = GradientDrawable.RECTANGLE
            cornerRadius  = 32f
            setColor(color)
        }
        holder.tvPriority.background = pill

        holder.itemView.setOnClickListener { onClick(folder) }
        holder.btnDelete.setOnClickListener { onDeleteClick(folder) }
    }

    private fun taskCountLabel(count: Int): String = when (count % 100) {
        in 11..19 -> "$count задач"
        else -> when (count % 10) {
            1    -> "$count задача"
            2, 3, 4 -> "$count задачи"
            else -> "$count задач"
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Folder>() {
            override fun areItemsTheSame(a: Folder, b: Folder) = a.id == b.id
            override fun areContentsTheSame(a: Folder, b: Folder) = a == b
        }
    }
}
