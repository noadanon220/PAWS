package com.danono.paws.ui.mydogs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.NotesAdapter
import com.danono.paws.databinding.FragmentDogNotesBinding
import com.danono.paws.model.DogNote
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class DogNotesFragment : Fragment(R.layout.fragment_dog_notes) {

    private var _binding: FragmentDogNotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesAdapter: NotesAdapter
    private lateinit var sharedViewModel: SharedDogsViewModel
    private val notesList = mutableListOf<DogNote>()

    // Firebase instances for database operations
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentDogId: String = "" // Use dog ID instead of name
    private var currentDogName: String = "" // Keep name for display purposes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDogNotesBinding.bind(view)

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
                loadNotesFromFirebase(it)
            }
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
        val userId = auth.currentUser?.uid ?: return

        val note = DogNote(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            createdDate = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        // Save new note to Firebase using dog ID
        saveNoteToFirebase(userId, note)
    }

    private fun updateNote(note: DogNote, newTitle: String, newContent: String) {
        val userId = auth.currentUser?.uid ?: return

        val updatedNote = note.copy(
            title = newTitle,
            content = newContent,
            lastModified = System.currentTimeMillis()
        )

        // Update existing note in Firebase
        updateNoteInFirebase(userId, updatedNote)
    }

    private fun deleteNote(note: DogNote) {
        val userId = auth.currentUser?.uid ?: return

        // Remove note from Firebase
        deleteNoteFromFirebase(userId, note)
    }

    private fun loadNotesFromFirebase(dogId: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .document(dogId) // Use dog ID instead of name
            .collection("notes")
            .orderBy("lastModified", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                notesList.clear()
                for (document in documents) {
                    val note = document.toObject(DogNote::class.java)
                    notesList.add(note)
                }
                notesAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener { exception ->
                // Silently handle empty collections - show empty state
                notesList.clear()
                notesAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
    }

    private fun saveNoteToFirebase(userId: String, note: DogNote) {
        firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .document(currentDogId) // Use dog ID instead of name
            .collection("notes")
            .document(note.id)
            .set(note)
            .addOnSuccessListener {
                // Add to local list for immediate UI update
                notesList.add(0, note)
                notesAdapter.notifyItemInserted(0)
                binding.notesRecyclerView.scrollToPosition(0)
                updateEmptyState()

                Toast.makeText(requireContext(), "Note saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to save note: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateNoteInFirebase(userId: String, note: DogNote) {
        firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .document(currentDogId) // Use dog ID instead of name
            .collection("notes")
            .document(note.id)
            .set(note)
            .addOnSuccessListener {
                // Update local list to reflect changes immediately
                val index = notesList.indexOfFirst { it.id == note.id }
                if (index != -1) {
                    notesList[index] = note
                    notesAdapter.notifyItemChanged(index)
                }

                Toast.makeText(requireContext(), "Note updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to update note: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteNoteFromFirebase(userId: String, note: DogNote) {
        firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .document(currentDogId) // Use dog ID instead of name
            .collection("notes")
            .document(note.id)
            .delete()
            .addOnSuccessListener {
                // Remove from local list to update UI
                val index = notesList.indexOf(note)
                if (index != -1) {
                    notesList.removeAt(index)
                    notesAdapter.notifyItemRemoved(index)
                    updateEmptyState()
                }

                Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to delete note: ${exception.message}", Toast.LENGTH_LONG).show()
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
        _binding = null
    }
}