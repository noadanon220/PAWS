package com.danono.paws.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.RemindersAdapter
import com.danono.paws.adapters.CalendarRemindersAdapter
import com.danono.paws.databinding.FragmentRemindersBinding
import com.danono.paws.model.Reminder
import com.danono.paws.model.CalendarDayReminder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private lateinit var remindersAdapter: RemindersAdapter
    private lateinit var calendarAdapter: CalendarRemindersAdapter
    private lateinit var remindersViewModel: RemindersViewModel
    private val remindersList = mutableListOf<Reminder>()
    private val calendarDays = mutableListOf<CalendarDayReminder>()

    // Firebase instances
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Calendar variables
    private var selectedDate = ""
    private var selectedTime = ""
    private var calendar = Calendar.getInstance()
    private var currentSelectedDate = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        remindersViewModel = ViewModelProvider(this).get(RemindersViewModel::class.java)
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendar()
        setupRecyclerView()
        setupCalendarRecyclerView()
        setupFab()
        loadReminders()
        updateCalendarHeader()
        generateCalendarDays()

        // Set today as default selected date
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        currentSelectedDate = today
    }

    private fun setupCalendar() {
        // Month navigation
        binding.previousMonthButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendarHeader()
            generateCalendarDays()
        }

        binding.nextMonthButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendarHeader()
            generateCalendarDays()
        }
    }

    private fun setupCalendarRecyclerView() {
        calendarAdapter = CalendarRemindersAdapter(calendarDays) { selectedDay ->
            // Handle day selection
            handleDaySelection(selectedDay)
        }

        binding.calendarRecyclerView.apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
        }
    }

    private fun handleDaySelection(selectedDay: CalendarDayReminder) {
        // Update selection state
        calendarDays.forEach { it.isSelected = false }
        val dayIndex = calendarDays.indexOf(selectedDay)
        if (dayIndex != -1) {
            calendarDays[dayIndex] = selectedDay.copy(isSelected = true)
            currentSelectedDate = selectedDay.date
        }
        calendarAdapter.notifyDataSetChanged()

        // Filter reminders for selected date
        filterRemindersForDate(selectedDay.date)
    }

    private fun generateCalendarDays() {
        calendarDays.clear()

        val tempCalendar = Calendar.getInstance()
        tempCalendar.time = calendar.time
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dayNames = arrayOf("S", "M", "T", "W", "T", "F", "S")

        // Add empty cells for days before the first day of the month
        for (i in 0 until firstDayOfWeek) {
            calendarDays.add(
                CalendarDayReminder(
                    date = "",
                    dayName = "",
                    dayNumber = 0,
                    isToday = false,
                    isSelected = false,
                    hasReminders = false,
                    reminderCount = 0
                )
            )
        }

        // Add days of the month
        for (day in 1..daysInMonth) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, day)
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tempCalendar.time)
            val dayOfWeek = (firstDayOfWeek + day - 1) % 7

            val hasReminders = remindersList.any { reminder ->
                val reminderDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(reminder.dateTime))
                reminderDate == dateString
            }

            val reminderCount = remindersList.count { reminder ->
                val reminderDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(reminder.dateTime))
                reminderDate == dateString
            }

            calendarDays.add(
                CalendarDayReminder(
                    date = dateString,
                    dayName = dayNames[dayOfWeek],
                    dayNumber = day,
                    isToday = dateString == today,
                    isSelected = dateString == currentSelectedDate,
                    hasReminders = hasReminders,
                    reminderCount = reminderCount
                )
            )
        }

        calendarAdapter.notifyDataSetChanged()
    }

    private fun filterRemindersForDate(selectedDate: String) {
        if (selectedDate.isEmpty()) {
            // Show all reminders if no specific date selected
            remindersAdapter.notifyDataSetChanged()
            return
        }

        val filteredReminders = remindersList.filter { reminder ->
            val reminderDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(reminder.dateTime))
            reminderDate == selectedDate
        }

        // Update the adapter with filtered results
        // For now, we'll just refresh the whole list
        // In a more advanced implementation, you could create a filtered adapter
        remindersAdapter.notifyDataSetChanged()
    }

    private fun updateCalendarHeader() {
        val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        binding.monthYearText.text = monthYear
    }

    private fun setupRecyclerView() {
        remindersAdapter = RemindersAdapter(remindersList) { reminder ->
            // Handle reminder click (edit/view)
            showEditReminderDialog(reminder)
        }

        binding.remindersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = remindersAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddReminder.setOnClickListener {
            showAddReminderDialog()
        }
    }

    private fun loadReminders() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                remindersList.clear()
                for (document in documents) {
                    val reminder = document.toObject(Reminder::class.java)
                    remindersList.add(reminder)
                }
                remindersAdapter.notifyDataSetChanged()
                updateEmptyState()
                generateCalendarDays() // Refresh calendar to show reminder indicators
            }
            .addOnFailureListener {
                remindersList.clear()
                remindersAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
    }

    private fun showAddReminderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.reminderTitle)
        val locationInput = dialogView.findViewById<TextInputEditText>(R.id.reminderLocation)
        val dateButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.datePickerButton)
        val timeButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.timePickerButton)

        // Reset selected date and time
        selectedDate = ""
        selectedTime = ""

        // Date picker
        dateButton.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                dateButton.text = date
            }
        }

        // Time picker
        timeButton.setOnClickListener {
            showTimePicker { time ->
                selectedTime = time
                timeButton.text = time
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Reminder")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val location = locationInput.text.toString().trim()

                if (title.isNotEmpty() && selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
                    addReminder(title, location, selectedDate, selectedTime)
                } else {
                    Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditReminderDialog(reminder: Reminder) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.reminderTitle)
        val locationInput = dialogView.findViewById<TextInputEditText>(R.id.reminderLocation)
        val dateButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.datePickerButton)
        val timeButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.timePickerButton)

        // Pre-fill with existing data
        titleInput.setText(reminder.title)
        locationInput.setText(reminder.location)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val reminderDate = Date(reminder.dateTime)

        selectedDate = dateFormat.format(reminderDate)
        selectedTime = timeFormat.format(reminderDate)

        dateButton.text = selectedDate
        timeButton.text = selectedTime

        // Date picker
        dateButton.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                dateButton.text = date
            }
        }

        // Time picker
        timeButton.setOnClickListener {
            showTimePicker { time ->
                selectedTime = time
                timeButton.text = time
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Reminder")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val title = titleInput.text.toString().trim()
                val location = locationInput.text.toString().trim()

                if (title.isNotEmpty() && selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
                    updateReminder(reminder, title, location, selectedDate, selectedTime)
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                deleteReminder(reminder)
            }
            .show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val dateCalendar = Calendar.getInstance()
            dateCalendar.set(selectedYear, selectedMonth, selectedDay)

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(dateCalendar.time)
            onDateSelected(formattedDate)
        }, year, month, day).show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val time = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(time)
        }, hour, minute, true).show()
    }

    private fun addReminder(title: String, location: String, date: String, time: String) {
        val userId = auth.currentUser?.uid ?: return

        // Parse date and time to create timestamp
        val dateTimeString = "$date $time"
        val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val dateTime = dateTimeFormat.parse(dateTimeString)?.time ?: System.currentTimeMillis()

        val reminder = Reminder(
            id = UUID.randomUUID().toString(),
            title = title,
            location = location,
            dateTime = dateTime,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )

        saveReminderToFirebase(userId, reminder)
    }

    private fun updateReminder(reminder: Reminder, newTitle: String, newLocation: String, newDate: String, newTime: String) {
        val userId = auth.currentUser?.uid ?: return

        // Parse date and time to create timestamp
        val dateTimeString = "$newDate $newTime"
        val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val dateTime = dateTimeFormat.parse(dateTimeString)?.time ?: reminder.dateTime

        val updatedReminder = reminder.copy(
            title = newTitle,
            location = newLocation,
            dateTime = dateTime
        )

        updateReminderInFirebase(userId, updatedReminder)
    }

    private fun deleteReminder(reminder: Reminder) {
        val userId = auth.currentUser?.uid ?: return
        deleteReminderFromFirebase(userId, reminder)
    }

    private fun saveReminderToFirebase(userId: String, reminder: Reminder) {
        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminder.id)
            .set(reminder)
            .addOnSuccessListener {
                remindersList.add(reminder)
                remindersList.sortBy { it.dateTime }
                remindersAdapter.notifyDataSetChanged()
                updateEmptyState()
                generateCalendarDays() // Refresh calendar to show new reminder
                Toast.makeText(requireContext(), "Reminder saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to save reminder: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateReminderInFirebase(userId: String, reminder: Reminder) {
        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminder.id)
            .set(reminder)
            .addOnSuccessListener {
                val index = remindersList.indexOfFirst { it.id == reminder.id }
                if (index != -1) {
                    remindersList[index] = reminder
                    remindersList.sortBy { it.dateTime }
                    remindersAdapter.notifyDataSetChanged()
                }
                Toast.makeText(requireContext(), "Reminder updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to update reminder: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteReminderFromFirebase(userId: String, reminder: Reminder) {
        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminder.id)
            .delete()
            .addOnSuccessListener {
                val index = remindersList.indexOf(reminder)
                if (index != -1) {
                    remindersList.removeAt(index)
                    remindersAdapter.notifyItemRemoved(index)
                    updateEmptyState()
                }
                Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to delete reminder: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateEmptyState() {
        if (remindersList.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.remindersRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.remindersRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}