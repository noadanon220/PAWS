package com.danono.paws.ui.mydogs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.NotesAdapter
import com.danono.paws.databinding.FragmentDogNotesBinding
import com.danono.paws.model.DogNote
import com.danono.paws.utilities.FirebaseDataManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.*

class DogNotesFragment : Fragment(R.layout.fragment_dog_notes) {

    private var _binding: FragmentDogNotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesAdapter: NotesAdapter
    private lateinit var sharedViewModel: SharedDogsViewModel
    private lateinit var firebaseDataManager: FirebaseDataManager
    private val notesList = mutableListOf<DogNote>()

    private var currentDogId: String = ""
    private var currentDogName: String = ""
    private var notesListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDogNotesBinding.bind(view)

        // Initialize Firebase Data Manager and ViewModel
        firebaseDataManager = FirebaseDataManager.getInstance()
        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]

        setupRecyclerView()
        setupFab()
        observeSelectedDog()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(notesList) { note ->
            showEditNoteDialog(note)
        }

        binding.notesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notesAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            showAddNoteDialog()
        }
    }

    private fun observeSelectedDog() {
        // Observe both dog and dogId
        sharedViewModel.selectedDog.observe(viewLifecycleOwner) { dog ->
            dog?.let {
                currentDogName = dog.name
                binding.dogNameTitle.text = "${dog.name}'s Notes"
            }
        }

        sharedViewModel.selectedDogId.observe(viewLifecycleOwner) { dogId ->
            dogId?.let {
                currentDogId = it
                setupNotesListener(it)
            }
        }
    }

    /**
     * Setup real-time listener for notes using FirebaseDataManager
     */
    private fun setupNotesListener(dogId: String) {
        // Remove previous listener if exists
        notesListener?.remove()

        // Setup new listener
        notesListener = firebaseDataManager.addNotesListener(dogId) { notes ->
            notesList.clear()
            notesList.addAll(notes)
            notesAdapter.notifyDataSetChanged()
            updateEmptyState()
        }

        if (notesListener == null) {
            // Fallback to loading notes once if listener setup fails
            loadNotesFromFirebase(dogId)
        }
    }

    /**
     * Fallback method to load notes if listener fails
     */
    private fun loadNotesFromFirebase(dogId: String) {
        lifecycleScope.launch {
            val result = firebaseDataManager.getNotes(dogId)
            result.fold(
                onSuccess = { notes ->
                    notesList.clear()
                    notesList.addAll(notes)
                    notesAdapter.notifyDataSetChanged()
                    updateEmptyState()
                },
                onFailure = {
                    // Handle empty collections gracefully - show empty state
                    notesList.clear()
                    notesAdapter.notifyDataSetChanged()
                    updateEmptyState()
                }
            )
        }
    }

    private fun showAddNoteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.noteTitle)
        val contentInput = dialogView.findViewById<TextInputEditText>(R.id.noteContent)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Note")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val content = contentInput.text.toString().trim()

                if (title.isNotEmpty() && content.isNotEmpty()) {
                    addNote(title, content)
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditNoteDialog(note: DogNote) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.noteTitle)
        val contentInput = dialogView.findViewById<TextInputEditText>(R.id.noteContent)

        titleInput.setText(note.title)
        contentInput.setText(note.content)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Note")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val title = titleInput.text.toString().trim()
                val content = contentInput.text.toString().trim()

                if (title.isNotEmpty() && content.isNotEmpty()) {
                    updateNote(note, title, content)
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                deleteNote(note)
            }
            .show()
    }

    private fun addNote(title: String, content: String) {
        val note = DogNote(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            createdDate = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            val result = firebaseDataManager.addNote(currentDogId, note)
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Note saved", Toast.LENGTH_SHORT).show()
                    // The listener will automatically update the UI
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to save note: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun updateNote(note: DogNote, newTitle: String, newContent: String) {
        val updatedNote = note.copy(
            title = newTitle,
            content = newContent,
            lastModified = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            val result = firebaseDataManager.updateNote(currentDogId, updatedNote)
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Note updated", Toast.LENGTH_SHORT).show()
                    // The listener will automatically update the UI
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to update note: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun deleteNote(note: DogNote) {
        lifecycleScope.launch {
            val result = firebaseDataManager.deleteNote(currentDogId, note.id)
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show()
                    // The listener will automatically update the UI
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to delete note: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun updateEmptyState() {
        if (notesList.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.notesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.notesRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the listener when view is destroyed
        notesListener?.remove()
        _binding = null
    }
}