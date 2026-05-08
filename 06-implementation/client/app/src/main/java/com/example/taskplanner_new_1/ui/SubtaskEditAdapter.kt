package com.example.taskplanner_new_1.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.taskplanner_new_1.R
import com.example.taskplanner_new_1.data.Subtask

class SubtaskEditAdapter(
    private val subtaskList: MutableList<Subtask>
) : RecyclerView.Adapter<SubtaskEditAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subtask, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(subtaskList[position])
    }

    override fun getItemCount() = subtaskList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleEdit: EditText = itemView.findViewById(R.id.subtask_title_edit)
        private val removeButton: ImageButton = itemView.findViewById(R.id.remove_subtask_button)

        fun bind(subtask: Subtask) {
            titleEdit.setText(subtask.title)
            titleEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        subtaskList[pos] = subtask.copy(title = s.toString())
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            removeButton.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    subtaskList.removeAt(pos)
                    notifyItemRemoved(pos)
                }
            }
        }
    }
}