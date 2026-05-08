package com.example.taskplanner_new_1.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.taskplanner_new_1.CreateFolderActivity
import com.example.taskplanner_new_1.R
import com.example.taskplanner_new_1.data.Folder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FolderListFragment : Fragment() {

    private val viewModel: FolderViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var adapter: FolderListAdapter

    private val createFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadFolders()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_folder_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FolderListAdapter(
            onClick = { folder -> openFolder(folder) },
            onDeleteClick = { folder -> confirmDeleteFolder(folder) }
        )

        view.findViewById<RecyclerView>(R.id.folder_recycler_view).adapter = adapter

        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            adapter.submitList(folders)
            view.findViewById<View>(R.id.empty_state).visibility =
                if (folders.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadFolders()

        view.findViewById<FloatingActionButton>(R.id.fab_add_folder).setOnClickListener {
            createFolderLauncher.launch(
                Intent(requireContext(), CreateFolderActivity::class.java)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFolders()
    }

    private fun openFolder(folder: Folder) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FolderTasksFragment.newInstance(folder.id, folder.name))
            .addToBackStack(null)
            .commit()
    }

    private fun confirmDeleteFolder(folder: Folder) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить папку?")
            .setMessage("«${folder.name}» и все задачи в ней будут удалены безвозвратно.")
            .setPositiveButton("Удалить") { _, _ -> viewModel.deleteFolder(folder.id) }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
