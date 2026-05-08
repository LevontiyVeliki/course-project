package com.example.taskplanner_new_1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskplanner_new_1.R
import com.example.taskplanner_new_1.data.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TaskListFragment : Fragment() {

    private var rootView: View? = null
    private val viewModel: TaskViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var adapter: TaskListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_task_list, container, false)
        return rootView!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TaskListAdapter(
            onTaskClick = { task ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, TaskEditFragment.newInstance(task.id))
                    .addToBackStack(null)
                    .commit()
            },
            onTaskCheckedChange = { task, isChecked ->
                viewModel.updateTask(task.copy(isDone = isChecked))
            },
            onDeleteClick = { task ->
                confirmDelete(task)
            }
        )

        val recycler = view.findViewById<RecyclerView>(R.id.task_recycler_view)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TaskEditFragment.newInstance(-1L))
                .addToBackStack(null)
                .commit()
        }
    }

    /** Диалог подтверждения перед удалением задачи. */
    private fun confirmDelete(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить задачу?")
            .setMessage("«${task.title}» будет удалена безвозвратно.")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteTask(task.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
    }
}
