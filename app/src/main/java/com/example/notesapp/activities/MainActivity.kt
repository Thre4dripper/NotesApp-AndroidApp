package com.example.notesapp.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.notesapp.R
import com.example.notesapp.adapters.HomeRecyclerAdapter
import com.example.notesapp.dataModels.Notes
import com.example.notesapp.databinding.ActivityMainBinding
import com.example.notesapp.viewModels.HomeViewModel

class MainActivity : AppCompatActivity(), HomeRecyclerAdapter.CardOnClickInterface {

    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: HomeRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        init()

        binding.homeFab.setOnClickListener {
            startAddNoteActivity()
        }
    }

    /**================================== METHOD FOR INITIALIZING TOOLBAR AND RECYCLER VIEW ==================================**/
    private fun init() {
        binding.homeToolbar.title = "Notes"
        binding.homeToolbar.setNavigationIcon(R.drawable.ic_home_menu)

        adapter = HomeRecyclerAdapter(homeViewModel.notesList.value!!, this)
        binding.homeRv.adapter = adapter
    }

    /**==================================== CALLBACK FOR RECEIVING NOTE ADD ACTIVITY RESULT ====================================**/
    private var addNoteCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it?.resultCode == Activity.RESULT_OK) {
                val noteTitle = it.data?.getStringExtra(AddNoteActivity.SEND_BACK_TITLE_KEY)
                val noteBody = it.data?.getStringExtra(AddNoteActivity.SEND_BACK_BODY_KEY)

                if (!noteTitle.isNullOrEmpty() || !noteBody.isNullOrEmpty()) {
                    homeViewModel.addNote(Notes(noteTitle!!, noteBody!!, false))
                    adapter.notifyItemInserted(homeViewModel.notesList.value!!.size)
                }
            }
        }

    private var editNoteCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it?.resultCode == Activity.RESULT_OK) {
                val noteTitle = it.data?.getStringExtra(AddNoteActivity.SEND_BACK_TITLE_KEY)
                val noteBody = it.data?.getStringExtra(AddNoteActivity.SEND_BACK_BODY_KEY)
                val noteIndex = it.data?.getIntExtra(AddNoteActivity.SEND_BACK_INDEX_KEY, -1)!!

                if (!noteTitle.isNullOrEmpty() || !noteBody.isNullOrEmpty()) {
                    homeViewModel.editNote(Notes(noteTitle!!, noteBody!!, false), noteIndex)
                    adapter.notifyItemChanged(noteIndex)
                }
            }
        }

    /**================================== METHOD FOR STARTING ADD NOTE ACTIVITY =====================================**/
    private fun startAddNoteActivity() {
        val intent = Intent(this, AddNoteActivity::class.java)
        addNoteCallback.launch(intent)
    }

    override fun cardOnClick(position: Int) {
        val intent = Intent(this, AddNoteActivity::class.java)

        intent.putExtra(
            HomeViewModel.NOTE_TITLE_KEY,
            homeViewModel.notesList.value?.get(position)?.head
        )

        intent.putExtra(
            HomeViewModel.NOTE_BODY_KEY,
            homeViewModel.notesList.value?.get(position)?.body
        )

        intent.putExtra(HomeViewModel.NOTE_INDEX_KEY, position)
        editNoteCallback.launch(intent)
    }
}