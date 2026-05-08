package com.example.taskplanner_new_1.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskplanner_new_1.R
import com.example.taskplanner_new_1.data.Task

class TaskListAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskCheckedChange: (Task, Boolean) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskListAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(itemView, onTaskClick, onTaskCheckedChange, onDeleteClick)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskViewHolder(
        itemView: View,
        private val onTaskClick: (Task) -> Unit,
        private val onTaskCheckedChange: (Task, Boolean) -> Unit,
        private val onDeleteClick: (Task) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView    = itemView.findViewById(R.id.task_title)
        private val dateView: TextView     = itemView.findViewById(R.id.task_date)
        private val timeView: TextView     = itemView.findViewById(R.id.task_time)
        private val statusView: CheckBox   = itemView.findViewById(R.id.task_status)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.task_delete_button)

        fun bind(task: Task) {
            titleView.text = task.title
            dateView.text  = task.date
            timeView.text  = task.time

            // Снимаем старый листенер перед setChecked, чтобы не было ложных срабатываний
            statusView.setOnCheckedChangeListener(null)
            statusView.isChecked = task.isDone
            statusView.setOnCheckedChangeListener { _, isChecked ->
                onTaskCheckedChange(task, isChecked)
            }

            itemView.setOnClickListener { onTaskClick(task) }
            deleteButton.setOnClickListener { onDeleteClick(task) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
