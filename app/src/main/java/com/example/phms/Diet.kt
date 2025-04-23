package com.example.phms

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

// Data model for a food item
data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val calories: Int = 0,
    val quantity: Int = 1,
    val totalCalories: Int = 0
)

// Data model for a diet entry (representing a day)
data class DietEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: Date = Date(),
    val foodItems: MutableList<FoodItem> = mutableListOf(),
    var totalCalories: Int = 0,
    var weight: Double? = null
)

class Diet : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var dietRecyclerView: RecyclerView
    private lateinit var addEntryButton: Button
    private lateinit var dateFilterEditText: EditText
    private lateinit var adapter: DietAdapter
    private val dietEntries = mutableListOf<DietEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diet)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        dietRecyclerView = findViewById(R.id.dietRecyclerView)
        addEntryButton = findViewById(R.id.addDietEntryButton)
        dateFilterEditText = findViewById(R.id.dateFilterEditText)

        setupRecyclerView()
        setupListeners()
        loadDietEntries()
    }

    private fun setupRecyclerView() {
        adapter = DietAdapter(dietEntries) { dietEntry ->
            showDietEntryDialog(dietEntry)
        }
        dietRecyclerView.layoutManager = LinearLayoutManager(this)
        dietRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        addEntryButton.setOnClickListener {
            showAddDietEntryDialog()
        }

        dateFilterEditText.setOnClickListener {
            // Date picker implementation
        }
    }

    private fun loadDietEntries() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .collection("diet")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    dietEntries.clear()
                    for (document in documents) {
                        val entry = document.toObject(DietEntry::class.java)
                        dietEntries.add(entry)
                    }
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error loading diet entries", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showAddDietEntryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_diet_entry, null)
        val dateEditText = dialogView.findViewById<EditText>(R.id.dateEditText)
        val weightEditText = dialogView.findViewById<EditText>(R.id.weightEditText)

        // Set current date
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateEditText.setText(sdf.format(Date()))

        AlertDialog.Builder(this)
            .setTitle("Add Diet Entry")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                try {
                    val date = sdf.parse(dateEditText.text.toString()) ?: Date()
                    val weight = if (weightEditText.text.isNotEmpty())
                        weightEditText.text.toString().toDouble() else null

                    val dietEntry = DietEntry(
                        date = date,
                        weight = weight
                    )

                    saveDietEntry(dietEntry)
                    // After saving, show dialog to add food items
                    showAddFoodItemDialog(dietEntry)
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDietEntryDialog(dietEntry: DietEntry) {
        val options = arrayOf("Add Food Item", "View Food Items", "Update Weight", "Delete Entry")

        AlertDialog.Builder(this)
            .setTitle("Diet Entry: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dietEntry.date)}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddFoodItemDialog(dietEntry)
                    1 -> showFoodItemsList(dietEntry)
                    2 -> showUpdateWeightDialog(dietEntry)
                    3 -> confirmDeleteDietEntry(dietEntry)
                }
            }
            .show()
    }

    private fun showAddFoodItemDialog(dietEntry: DietEntry) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_food_item, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.foodNameEditText)
        val caloriesEditText = dialogView.findViewById<EditText>(R.id.caloriesEditText)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)

        // Default quantity to 1
        quantityEditText.setText("1")

        AlertDialog.Builder(this)
            .setTitle("Add Food Item")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                try {
                    val name = nameEditText.text.toString().trim()
                    val calories = caloriesEditText.text.toString().toInt()
                    val quantity = quantityEditText.text.toString().toInt()
                    val totalCalories = calories * quantity

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Food name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val foodItem = FoodItem(
                        name = name,
                        calories = calories,
                        quantity = quantity,
                        totalCalories = totalCalories
                    )

                    addFoodItemToDietEntry(dietEntry, foodItem)
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFoodItemsList(dietEntry: DietEntry) {
        if (dietEntry.foodItems.isEmpty()) {
            Toast.makeText(this, "No food items found", Toast.LENGTH_SHORT).show()
            return
        }

        val foodItemsArray = dietEntry.foodItems.map {
            "${it.name} (${it.quantity}) - ${it.totalCalories} calories"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Food Items")
            .setItems(foodItemsArray) { _, which ->
                val foodItem = dietEntry.foodItems[which]
                showFoodItemOptionsDialog(dietEntry, foodItem)
            }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showFoodItemOptionsDialog(dietEntry: DietEntry, foodItem: FoodItem) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(this)
            .setTitle(foodItem.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditFoodItemDialog(dietEntry, foodItem)
                    1 -> confirmDeleteFoodItem(dietEntry, foodItem)
                }
            }
            .show()
    }

    private fun showEditFoodItemDialog(dietEntry: DietEntry, foodItem: FoodItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_food_item, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.foodNameEditText)
        val caloriesEditText = dialogView.findViewById<EditText>(R.id.caloriesEditText)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)

        nameEditText.setText(foodItem.name)
        caloriesEditText.setText(foodItem.calories.toString())
        quantityEditText.setText(foodItem.quantity.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit Food Item")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                try {
                    val name = nameEditText.text.toString().trim()
                    val calories = caloriesEditText.text.toString().toInt()
                    val quantity = quantityEditText.text.toString().toInt()
                    val totalCalories = calories * quantity

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Food name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val updatedFoodItem = FoodItem(
                        id = foodItem.id,
                        name = name,
                        calories = calories,
                        quantity = quantity,
                        totalCalories = totalCalories
                    )

                    updateFoodItem(dietEntry, updatedFoodItem)
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUpdateWeightDialog(dietEntry: DietEntry) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_weight, null)
        val weightEditText = dialogView.findViewById<EditText>(R.id.weightEditText)

        dietEntry.weight?.let {
            weightEditText.setText(it.toString())
        }

        AlertDialog.Builder(this)
            .setTitle("Update Weight")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                try {
                    val weight = if (weightEditText.text.isNotEmpty())
                        weightEditText.text.toString().toDouble() else null

                    updateDietEntryWeight(dietEntry, weight)
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteDietEntry(dietEntry: DietEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Diet Entry")
            .setMessage("Are you sure you want to delete this diet entry?")
            .setPositiveButton("Yes") { _, _ ->
                deleteDietEntry(dietEntry)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun confirmDeleteFoodItem(dietEntry: DietEntry, foodItem: FoodItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Food Item")
            .setMessage("Are you sure you want to delete ${foodItem.name}?")
            .setPositiveButton("Yes") { _, _ ->
                deleteFoodItem(dietEntry, foodItem)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun saveDietEntry(dietEntry: DietEntry) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .collection("diet")
                .document(dietEntry.id)
                .set(dietEntry)
                .addOnSuccessListener {
                    loadDietEntries()
                    Toast.makeText(this, "Diet entry saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error saving diet entry", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateDietEntry(dietEntry: DietEntry) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .collection("diet")
                .document(dietEntry.id)
                .set(dietEntry)
                .addOnSuccessListener {
                    loadDietEntries()
                    Toast.makeText(this, "Diet entry updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error updating diet entry", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteDietEntry(dietEntry: DietEntry) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .collection("diet")
                .document(dietEntry.id)
                .delete()
                .addOnSuccessListener {
                    loadDietEntries()
                    Toast.makeText(this, "Diet entry deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error deleting diet entry", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addFoodItemToDietEntry(dietEntry: DietEntry, foodItem: FoodItem) {
        dietEntry.foodItems.add(foodItem)
        recalculateTotalCalories(dietEntry)
        updateDietEntry(dietEntry)
    }

    private fun updateFoodItem(dietEntry: DietEntry, updatedFoodItem: FoodItem) {
        val index = dietEntry.foodItems.indexOfFirst { it.id == updatedFoodItem.id }
        if (index != -1) {
            dietEntry.foodItems[index] = updatedFoodItem
            recalculateTotalCalories(dietEntry)
            updateDietEntry(dietEntry)
        }
    }

    private fun deleteFoodItem(dietEntry: DietEntry, foodItem: FoodItem) {
        dietEntry.foodItems.removeIf { it.id == foodItem.id }
        recalculateTotalCalories(dietEntry)
        updateDietEntry(dietEntry)
    }

    private fun updateDietEntryWeight(dietEntry: DietEntry, weight: Double?) {
        dietEntry.weight = weight
        updateDietEntry(dietEntry)
    }

    private fun recalculateTotalCalories(dietEntry: DietEntry) {
        dietEntry.totalCalories = dietEntry.foodItems.sumOf { it.totalCalories }
    }
}