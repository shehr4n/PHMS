package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", 0L)
}

class Notes : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var notesList: RecyclerView
    private lateinit var addNoteButton: Button
    private lateinit var adapter: NotesAdapter
    private val notes = mutableListOf<Note>()
    lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        notesList = findViewById(R.id.notesList)
        addNoteButton = findViewById(R.id.addNoteButton)

        adapter = NotesAdapter(notes,
            onEdit = { showEditNoteDialog(it) },
            onDelete = { deleteNote(it) }
        )
        notesList.layoutManager = LinearLayoutManager(this)
        notesList.adapter = adapter

        addNoteButton.setOnClickListener {
            showEditNoteDialog(null)
        }

        loadNotes()

        backButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }
    }

    private fun loadNotes() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("notes")
            .get()
            .addOnSuccessListener { result ->
                notes.clear()
                for (doc in result) {
                    val note = doc.toObject(Note::class.java)
                    notes.add(note)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showEditNoteDialog(existingNote: Note?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note, null)
        val noteInput = dialogView.findViewById<EditText>(R.id.noteInput)
        noteInput.setText(existingNote?.content ?: "")

        AlertDialog.Builder(this)
            .setTitle(if (existingNote == null) "New Note" else "Edit Note")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val content = noteInput.text.toString()
                val note = existingNote?.copy(content = content, timestamp = System.currentTimeMillis())
                    ?: Note(content = content)
                saveNote(note)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNote(note: Note) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("notes").document(note.id)
            .set(note)
            .addOnSuccessListener {
                loadNotes()
                Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteNote(note: Note) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("notes").document(note.id)
            .delete()
            .addOnSuccessListener {
                loadNotes()
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show()
            }
    }
}

class NotesAdapter(
    private var notes: List<Note>,
    private val onEdit: (Note) -> Unit,
    private val onDelete: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val contentText: TextView = view.findViewById(R.id.noteContent)
        val timestampText: TextView = view.findViewById(R.id.noteTimestamp)
        val editBtn: ImageButton = view.findViewById(R.id.editNote)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteNote)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): NoteViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.contentText.text = note.content
        holder.timestampText.text = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            .format(Date(note.timestamp))

        holder.editBtn.setOnClickListener { onEdit(note) }
        holder.deleteBtn.setOnClickListener { onDelete(note) }
    }

    override fun getItemCount(): Int = notes.size
}
