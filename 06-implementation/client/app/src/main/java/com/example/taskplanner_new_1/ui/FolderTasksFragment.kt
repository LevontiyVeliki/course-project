package com.example.taskplanner_new_1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.taskplanner_new_1.R
import com.example.taskplanner_new_1.data.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FolderTasksFragment : Fragment() {

    private val viewModel: TaskViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var adapter: TaskListAdapter
    private var folderId: Long   = -1L
    private var folderName: String = ""

    companion object {
        private const val ARG_FOLDER_ID   = "folder_id"
        private const val ARG_FOLDER_NAME = "folder_name"

        fun newInstance(folderId: Long, folderName: String): FolderTasksFragment {
            val fragment = FolderTasksFragment()
            fragment.arguments = Bundle().apply {
                putLong(ARG_FOLDER_ID, folderId)
                putString(ARG_FOLDER_NAME, folderName)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        folderId   = arguments?.getLong(ARG_FOLDER_ID, -1L) ?: -1L
        folderName = arguments?.getString(ARG_FOLDER_NAME) ?: "Задачи"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_folder_tasks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Используем Activity ActionBar — показываем имя папки и стрелку «Назад»
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.let { bar ->
            bar.title = folderName
            bar.setDisplayHomeAsUpEnabled(true)
        }

        adapter = TaskListAdapter(
            onTaskClick = { task ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, TaskEditFragment.newInstance(task.id, folderId))
                    .addToBackStack(null)
                    .commit()
            },
            onTaskCheckedChange = { task, isChecked ->
                viewModel.updateTask(task.copy(isDone = isChecked))
            },
            onDeleteClick = { task -> confirmDelete(task) }
        )

        view.findViewById<RecyclerView>(R.id.task_recycler_view).adapter = adapter

        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
            view.findViewById<View>(R.id.folder_empty_state).visibility =
                if (tasks.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadTasksForFolder(folderId)

        view.findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TaskEditFragment.newInstance(-1L, folderId))
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        // Восстанавливаем заголовок и стрелку на случай возврата с TaskEditFragment
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.let { bar ->
            bar.title = folderName
            bar.setDisplayHomeAsUpEnabled(true)
        }
        viewModel.loadTasksForFolder(folderId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Возвращаем ActionBar в исходное состояние для FolderListFragment
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.let { bar ->
            bar.title = "Планировщик"
            bar.setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun confirmDelete(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить задачу?")
            .setMessage("«${task.title}» будет удалена безвозвратно.")
            .setPositiveButton("Удалить") { _, _ -> viewModel.deleteTask(task.id) }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
