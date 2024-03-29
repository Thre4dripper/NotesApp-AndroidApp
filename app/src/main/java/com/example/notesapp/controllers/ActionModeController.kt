package com.example.notesapp.controllers

import android.text.Html
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import com.example.notesapp.R
import com.example.notesapp.adapters.HomeRecyclerAdapter
import com.example.notesapp.dataModels.Notes
import com.example.notesapp.databinding.ActivityMainBinding
import com.example.notesapp.databinding.ColorPaletteBinding
import com.example.notesapp.viewModels.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class ActionModeController(
    private val homeViewModel: HomeViewModel,
    private val adapter: HomeRecyclerAdapter,
    private val binding: ActivityMainBinding
) {

    private val isNotesSection = binding.homeToolbar.title == "Notes"

    /**=================================================== CALLBACK FOR CONTEXTUAL MENU =============================================**/
    inner class ActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
            p0!!.menuInflater.inflate(R.menu.menu_contextual, p1)

            //changing archive icon when in archives section
            if (!isNotesSection)
                p1!!.findItem(R.id.contextual_archive).setIcon(R.drawable.ic_unarchive_note)

            return true
        }

        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
            return true
        }

        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
            actionItemClicked(p1)
            return true
        }

        override fun onDestroyActionMode(p0: ActionMode?) {
            actionModeDestroyed()
        }
    }

    /**==================================== METHOD FOR HANDLING ACTION ITEMS CLICKS =======================================**/
    private fun actionItemClicked(p1: MenuItem?) {
        var removedNoteIndex: Int
        val removedNoteIndexList: MutableList<Int> = mutableListOf()

        when (p1!!.itemId) {
            R.id.contextual_delete -> {

                //filling index list first, cause notes list size will vary when deleting notes
                for (item in homeViewModel.selectedItems) {
                    removedNoteIndex = homeViewModel.displayNotesList.indexOf(item)
                    removedNoteIndexList.add(removedNoteIndex)
                }

                //removing only selected items one by one
                for (item in homeViewModel.selectedItems) {
                    removedNoteIndex = homeViewModel.displayNotesList.indexOf(item)
                    homeViewModel.deleteDisplayNote(removedNoteIndex)
                    adapter.notifyItemRemoved(removedNoteIndex)
                }

                deleteSnackBar(removedNoteIndexList, homeViewModel.selectedItems)
                homeViewModel.selectedItems.clear()

                //hiding placeholder view when no items left
                if (homeViewModel.displayNotesList.size == 0)
                    binding.homePlaceholder.visibility = View.VISIBLE
            }
            R.id.contextual_archive -> {

                //filling index list first, cause notes list size will vary when deleting notes
                for (item in homeViewModel.selectedItems) {
                    removedNoteIndex = homeViewModel.displayNotesList.indexOf(item)
                    removedNoteIndexList.add(removedNoteIndex)
                }

                //removing only selected items one by one
                for (item in homeViewModel.selectedItems) {
                    removedNoteIndex = homeViewModel.displayNotesList.indexOf(item)
                    homeViewModel.deleteDisplayNote(removedNoteIndex)
                    adapter.notifyItemRemoved(removedNoteIndex)
                }

                archiveSnackBar(
                    removedNoteIndexList,
                    homeViewModel.selectedItems,
                    homeViewModel,
                    binding
                )
                homeViewModel.selectedItems.clear()

                //hiding placeholder view when no items left
                if (homeViewModel.displayNotesList.size == 0)
                    binding.homePlaceholder.visibility = View.VISIBLE
            }
            R.id.contextual_color -> {
                //filling index list first, cause notes list size will vary when deleting notes
                for (item in homeViewModel.selectedItems) {
                    removedNoteIndex = homeViewModel.displayNotesList.indexOf(item)
                    removedNoteIndexList.add(removedNoteIndex)
                }

                colorPicker(
                    removedNoteIndexList,
                    homeViewModel,
                    adapter
                )
            }
        }

        homeViewModel.actionMode!!.finish()
    }

    /**==================================== METHOD FOR HANDLING ACTION MODE DESTROYED ====================================**/
    private fun actionModeDestroyed() {
        for (item in homeViewModel.selectedItems) {
            item.isSelected = false

            //notifying adapter for only those items which have changed
            //by finding their index
            adapter.notifyItemChanged(homeViewModel.displayNotesList.indexOf(item))
        }

        //resetting selection list
        homeViewModel.selectedItems.clear()
        homeViewModel.selectionMode = false

        homeViewModel.actionMode = null
    }

    /**========================================= METHOD FOR HANDLING DELETING SNACK BAR =======================================**/
    private fun deleteSnackBar(
        indexList: MutableList<Int>,
        NotesList: MutableList<Notes>
    ) {

        //mapping indexes to notes list
        var notesMapList = mutableMapOf<Int, Notes>()
        for (i in 0 until indexList.size)
            notesMapList[indexList[i]] = NotesList[i]

        //sorting indexes for removing selection order
        indexList.sort()
        notesMapList = notesMapList.toSortedMap()

        //snackBar message string
        var snackBarMessage = if (isNotesSection) {
            if (notesMapList.size > 1) "Notes Deleted" else "Note Deleted"
        } else {
            if (notesMapList.size > 1) "Archives Deleted" else "Archive Deleted"
        }

        //making snack bar
        Snackbar.make(
            binding.homeRootLayout,
            snackBarMessage,
            Snackbar.LENGTH_LONG
        )

            //DISMISS LISTENER
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    indexList.clear()
                    notesMapList.clear()
                }
            })

            //SNACK BAR UNDO BUTTON
            .setAction("UNDO") {

                for (item in indexList) {
                    notesMapList[item]!!.isSelected = false
                    homeViewModel.addDisplayNoteAt(item, notesMapList[item]!!)
                    adapter.notifyItemInserted(item)
                }

                snackBarMessage = if (isNotesSection) {
                    if (notesMapList.size > 1) "Notes Recovered" else "Note Recovered"
                } else {
                    if (notesMapList.size > 1) "Archives Recovered" else "Archive Recovered"
                }

                Snackbar.make(
                    binding.homeRootLayout,
                    snackBarMessage,
                    Snackbar.LENGTH_SHORT
                ).show()

                //showing placeholder view on UNDO
                binding.homePlaceholder.visibility = View.GONE
            }
            .show()
    }

    /**========================================= METHOD FOR HANDLING ARCHIVE SNACK BAR =======================================**/
    private fun archiveSnackBar(
        indexList: MutableList<Int>,
        NotesList: MutableList<Notes>,
        homeViewModel: HomeViewModel,
        binding: ActivityMainBinding
    ) {

        //mapping indexes to notes list
        var notesMapList = mutableMapOf<Int, Notes>()
        for (i in 0 until indexList.size)
            notesMapList[indexList[i]] = NotesList[i]

        //sorting indexes for removing selection order
        indexList.sort()
        notesMapList = notesMapList.toSortedMap()

        //ADDING TO ARCHIVES/NOTES LIST
        for (item in indexList) {
            notesMapList[item]!!.isSelected = false

            //checking for section
            if (isNotesSection)
                homeViewModel.addToArchive(notesMapList[item]!!)
            else
                homeViewModel.addToNotes(notesMapList[item]!!)
        }

        //snackBar message string
        var snackBarMessage = if (isNotesSection) {
            if (notesMapList.size > 1) "Notes Archived" else "Note Archived"
        } else {
            if (notesMapList.size > 1) "Notes UnArchived" else "Note UnArchived"
        }

        //making snack bar
        val snackBar = Snackbar.make(
            binding.homeRootLayout,
            snackBarMessage,
            Snackbar.LENGTH_LONG
        )
            //DISMISS LISTENER
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    indexList.clear()
                    notesMapList.clear()
                }
            })
            //SNACK BAR UNDO BUTTON
            .setAction("UNDO") {


                for (item in indexList) {
                    //recovering archived/unarchived notes
                    notesMapList[item]!!.isSelected = false
                    homeViewModel.addDisplayNoteAt(item, notesMapList[item]!!)

                    //deleting notes on opposite end
                    //checking for section
                    if (isNotesSection)
                        homeViewModel.deleteFromArchived(notesMapList[item]!!)
                    else
                        homeViewModel.deleteFromNotes(notesMapList[item]!!)

                    adapter.notifyItemInserted(item)

                    //showing placeholder view on UNDO
                    binding.homePlaceholder.visibility = View.GONE
                }

                snackBarMessage = if (isNotesSection) {
                    if (notesMapList.size > 1) "Notes Recovered" else "Note Recovered"
                } else {
                    if (notesMapList.size > 1) "Archives Recovered" else "Archive Recovered"
                }

                Snackbar.make(
                    binding.homeRootLayout,
                    snackBarMessage,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        snackBar.show()


        //KILLING SNACK BAR WHEN NAVIGATION DRAWER IS OPENED FOR REMOVING UNDO FUNCTIONALITY
        binding.homeDrawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {
                snackBar.dismiss()
            }

            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    /**=========================================== METHOD FOR INFLATE COLOR PICKER =============================================**/
    private fun colorPicker(
        indexList: MutableList<Int>,
        model: HomeViewModel,
        adapter: HomeRecyclerAdapter
    ) {
        val context = this.binding.homeRootLayout.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.color_palette, null)

        val colorPicker = DataBindingUtil.bind<ColorPaletteBinding>(view)!!

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(Html.fromHtml("<h3><strong>Select Color</strong></h3>"))
            .setView(view)
            .show()

        colorPicker.cardView1.setOnClickListener {
            setColor(indexList, model, adapter, 0)
            dialog.dismiss()
        }
        colorPicker.cardView2.setOnClickListener {
            setColor(indexList, model, adapter, 1)
            dialog.dismiss()
        }
        colorPicker.cardView3.setOnClickListener {
            setColor(indexList, model, adapter, 2)
            dialog.dismiss()
        }
        colorPicker.cardView4.setOnClickListener {
            setColor(indexList, model, adapter, 3)
            dialog.dismiss()
        }
        colorPicker.cardView5.setOnClickListener {
            setColor(indexList, model, adapter, 4)
            dialog.dismiss()
        }
        colorPicker.cardView6.setOnClickListener {
            setColor(indexList, model, adapter, 5)
            dialog.dismiss()
        }
        colorPicker.cardView7.setOnClickListener {
            setColor(indexList, model, adapter, 6)
            dialog.dismiss()
        }
        colorPicker.cardView8.setOnClickListener {
            setColor(indexList, model, adapter, 7)
            dialog.dismiss()
        }
        colorPicker.cardView9.setOnClickListener {
            setColor(indexList, model, adapter, 8)
            dialog.dismiss()
        }
        colorPicker.cardView10.setOnClickListener {
            setColor(indexList, model, adapter, 9)
            dialog.dismiss()
        }
        colorPicker.cardView11.setOnClickListener {
            setColor(indexList, model, adapter, 10)
            dialog.dismiss()
        }
        colorPicker.cardView12.setOnClickListener {
            setColor(indexList, model, adapter, 11)
            dialog.dismiss()
        }
        colorPicker.cardView13.setOnClickListener {
            setColor(indexList, model, adapter, 12)
            dialog.dismiss()
        }
        colorPicker.cardView14.setOnClickListener {
            setColor(indexList, model, adapter, 13)
            dialog.dismiss()
        }
        colorPicker.cardView15.setOnClickListener {
            setColor(indexList, model, adapter, 14)
            dialog.dismiss()
        }
    }

    /**=============================== METHOD FOR SETTING NOTES COLOR ON SELECTED NOTES ==============================**/
    private fun setColor(
        removedNoteIndexList: MutableList<Int>,
        homeViewModel: HomeViewModel,
        adapter: HomeRecyclerAdapter,
        color: Int
    ) {
        for (i in removedNoteIndexList) {
            homeViewModel.setColor(i, color)
            adapter.notifyItemChanged(i)
        }
    }
}

