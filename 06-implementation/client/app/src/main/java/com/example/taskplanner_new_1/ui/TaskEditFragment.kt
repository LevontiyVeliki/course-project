package com.example.taskplanner_new_1.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskplanner_new_1.R
import com.example.taskplanner_new_1.data.Subtask
import com.example.taskplanner_new_1.data.Task
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.util.Calendar

class TaskEditFragment : Fragment() {

    private val viewModel: TaskViewModel by viewModels(ownerProducer = { requireActivity() })
    private var taskId: Long = -1L
    private var existingTask: Task? = null
    private val subtaskList = mutableListOf<Subtask>()
    private lateinit var subtaskAdapter: SubtaskEditAdapter

    // Cached view references — populated in onViewCreated, cleared in onDestroyView
    private var titleEdit: EditText? = null
    private var descEdit: EditText? = null
    private var dateEdit: EditText? = null
    private var timeEdit: EditText? = null

    companion object {
        private const val ARG_TASK_ID   = "task_id"
        private const val ARG_FOLDER_ID = "folder_id"

        fun newInstance(taskId: Long, folderId: Long = -1L): TaskEditFragment {
            val fragment = TaskEditFragment()
            fragment.arguments = Bundle().apply {
                putLong(ARG_TASK_ID,   taskId)
                putLong(ARG_FOLDER_ID, folderId)
            }
            return fragment
        }
    }

    private var folderId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId   = arguments?.getLong(ARG_TASK_ID,   -1L) ?: -1L
        folderId = arguments?.getLong(ARG_FOLDER_ID, -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_task_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Скрываем системный ActionBar — тулбар встроен прямо в лейаут
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.hide()

        // Настраиваем встроенный тулбар
        val toolbar = view.findViewById<MaterialToolbar>(R.id.edit_toolbar)
        toolbar.title = if (taskId > 0) "Редактировать задачу" else "Новая задача"
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        // Кэшируем ссылки на поля ввода
        titleEdit = view.findViewById(R.id.task_title_edit)
        descEdit  = view.findViewById(R.id.task_description_edit)
        dateEdit  = view.findViewById(R.id.task_date_edit)
        timeEdit  = view.findViewById(R.id.task_time_edit)

        subtaskAdapter = SubtaskEditAdapter(subtaskList)
        val subtaskRecycler = view.findViewById<RecyclerView>(R.id.subtask_recycler_view)
        subtaskRecycler.layoutManager = LinearLayoutManager(requireContext())
        subtaskRecycler.adapter = subtaskAdapter

        if (taskId > 0) {
            existingTask = viewModel.getTaskById(taskId)
            existingTask?.let { populateUI(it) }
        }

        dateEdit?.setOnClickListener { showDatePicker() }
        timeEdit?.setOnClickListener { showTimePicker() }

        view.findViewById<MaterialButton>(R.id.add_subtask_button).setOnClickListener {
            subtaskList.add(Subtask(taskId = taskId, title = ""))
            subtaskAdapter.notifyItemInserted(subtaskList.size - 1)
        }

        view.findViewById<MaterialButton>(R.id.save_task_button).setOnClickListener {
            saveTask()
        }

        view.findViewById<MaterialButton>(R.id.cancel_task_button).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun populateUI(task: Task) {
        titleEdit?.setText(task.title)
        descEdit?.setText(task.description)
        dateEdit?.setText(task.date)
        timeEdit?.setText(task.time)

        subtaskList.clear()
        subtaskList.addAll(task.subtasks)
        subtaskAdapter.notifyDataSetChanged()
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            dateEdit?.setText("$year-${String.format("%02d", month + 1)}-${String.format("%02d", day)}")
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hour, minute ->
            timeEdit?.setText("${String.format("%02d", hour)}:${String.format("%02d", minute)}")
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun saveTask() {
        titleEdit?.clearFocus()
        descEdit?.clearFocus()

        val title = titleEdit?.text?.toString()?.trim() ?: ""
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Введите название задачи", Toast.LENGTH_SHORT).show()
            return
        }

        val desc  = descEdit?.text?.toString()?.trim() ?: ""
        val date  = dateEdit?.text?.toString()?.trim() ?: ""
        val time  = timeEdit?.text?.toString()?.trim() ?: ""

        val task = Task(
            id          = if (taskId > 0) taskId else 0L,
            title       = title,
            description = desc,
            date        = date,
            time        = time,
            isDone      = existingTask?.isDone ?: false,
            folderId    = existingTask?.folderId ?: folderId
        )

        if (taskId > 0) {
            viewModel.updateTask(task)
        } else {
            val newId = viewModel.insertTask(task)
            taskId = newId
        }

        // Заменяем подзадачи: удаляем старые, вставляем непустые новые
        existingTask?.subtasks?.forEach { viewModel.deleteSubtask(it.id) }
        subtaskList.forEach { sub ->
            if (sub.title.isNotBlank()) {
                viewModel.insertSubtask(sub.copy(taskId = taskId))
            }
        }

        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Возвращаем системный ActionBar когда уходим с экрана
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.show()
        titleEdit = null
        descEdit  = null
        dateEdit  = null
        timeEdit  = null
    }
}
