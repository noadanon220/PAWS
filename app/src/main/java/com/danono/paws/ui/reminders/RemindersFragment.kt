package com.danono.paws.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.RemindersAdapter
import com.danono.paws.databinding.FragmentRemindersBinding
import com.danono.paws.model.Reminder
import com.danono.paws.model.ReminderType
import com.danono.paws.model.Dog
import com.danono.paws.ui.mydogs.SharedDogsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private lateinit var remindersAdapter: RemindersAdapter
    private lateinit var remindersViewModel: RemindersViewModel
    private lateinit var sharedDogsViewModel: SharedDogsViewModel
    private val remindersList = mutableListOf<Reminder>()
    private val dogsList = mutableListOf<Pair<Dog, String>>()

    // Firebase instances
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Calendar variables
    private var selectedDate = ""
    private var selectedTime = ""
    private var selectedCalendarDate: LocalDate? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        remindersViewModel = ViewModelProvider(this)[RemindersViewModel::class.java]
        sharedDogsViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendar()
        setupRecyclerView()
        setupFab()
        loadDogs()
        loadReminders()
        setupMonthNavigation()
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)

        binding.calendarView.setup(startMonth, endMonth, java.time.DayOfWeek.SUNDAY)
        binding.calendarView.scrollToMonth(currentMonth)

        // Update month/year text
        updateMonthYearText(currentMonth)

        // Day binder
        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.bind(data)
            }
        }

        // Month scroll listener
        binding.calendarView.monthScrollListener = { calendarMonth ->
            updateMonthYearText(calendarMonth.yearMonth)
        }
    }

    private fun setupMonthNavigation() {
        binding.previousMonthButton.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let { month ->
                binding.calendarView.scrollToMonth(month.yearMonth.minusMonths(1))
            }
        }

        binding.nextMonthButton.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let { month ->
                binding.calendarView.scrollToMonth(month.yearMonth.plusMonths(1))
            }
        }
    }

    private fun updateMonthYearText(yearMonth: YearMonth) {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        binding.monthYearText.text = yearMonth.format(formatter)
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val dayText = view.findViewById<android.widget.TextView>(R.id.calendarDayText)
        val indicator = view.findViewById<View>(R.id.calendarDayIndicator)

        fun bind(day: CalendarDay) {
            dayText.text = day.date.dayOfMonth.toString()

            // Reset styling
            dayText.setTextColor(resources.getColor(R.color.black, null))
            view.background = null
            indicator.visibility = View.GONE

            when (day.position) {
                DayPosition.MonthDate -> {
                    dayText.visibility = View.VISIBLE

                    // Check if day has reminders
                    val hasReminders = remindersList.any { reminder ->
                        val reminderDate = LocalDate.ofEpochDay(reminder.dateTime / (24 * 60 * 60 * 1000))
                        reminderDate == day.date
                    }

                    if (hasReminders) {
                        indicator.visibility = View.VISIBLE
                    }

                    // Highlight selected date
                    if (day.date == selectedCalendarDate) {
                        view.setBackgroundResource(R.drawable.bg_circle_selected)
                        dayText.setTextColor(resources.getColor(R.color.white, null))
                    }

                    // Highlight today
                    if (day.date == LocalDate.now()) {
                        if (day.date != selectedCalendarDate) {
                            dayText.setTextColor(resources.getColor(R.color.Primary_pink, null))
                        }
                    }
                }
                DayPosition.InDate, DayPosition.OutDate -> {
                    dayText.visibility = View.INVISIBLE
                }
            }

            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    selectDate(day.date)
                }
            }
        }
    }

    private fun selectDate(date: LocalDate) {
        selectedCalendarDate = date
        binding.calendarView.notifyCalendarChanged()

        // Show selected date info
        binding.selectedDateLayout.visibility = View.VISIBLE
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
        binding.selectedDateText.text = date.format(formatter)

        // Filter reminders for this date
        filterRemindersForDate(date)
    }

    private fun filterRemindersForDate(date: LocalDate) {
        val dateReminders = remindersList.filter { reminder ->
            val reminderDate = LocalDate.ofEpochDay(reminder.dateTime / (24 * 60 * 60 * 1000))
            reminderDate == date
        }

        binding.reminderCountText.text = "${dateReminders.size} reminders"

        // Update adapter with filtered reminders
        remindersAdapter = RemindersAdapter(dateReminders) { reminder ->
            showEditReminderDialog(reminder)
        }
        binding.remindersRecyclerView.adapter = remindersAdapter

        updateEmptyState(dateReminders.isEmpty())
    }

    private fun setupRecyclerView() {
        remindersAdapter = RemindersAdapter(remindersList) { reminder ->
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

    private fun loadDogs() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .get()
            .addOnSuccessListener { documents ->
                dogsList.clear()
                for (document in documents) {
                    val dog = document.toObject(Dog::class.java)
                    dogsList.add(Pair(dog, document.id))
                }
            }
    }

    private fun loadReminders() {
        val userId = auth.currentUser?.uid ?: return

        Log.d("RemindersFragment", "Loading reminders for user: $userId")

        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("RemindersFragment", "Found ${documents.size()} reminders")

                remindersList.clear()
                for (document in documents) {
                    val reminder = document.toObject(Reminder::class.java)
                    Log.d("RemindersFragment", "Reminder: ${reminder.title}, DateTime: ${reminder.dateTime}")
                    remindersList.add(reminder)
                }

                remindersAdapter.notifyDataSetChanged()
                binding.calendarView.notifyCalendarChanged()
                updateEmptyState(remindersList.isEmpty())

                Log.d("RemindersFragment", "Updated UI with ${remindersList.size} reminders")
            }
            .addOnFailureListener { exception ->
                Log.e("RemindersFragment", "Failed to load reminders", exception)
                remindersList.clear()
                remindersAdapter.notifyDataSetChanged()
                updateEmptyState(true)
            }
    }
    private fun showAddReminderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.reminderTitle)
        val typeSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.reminderTypeSpinner)
        val dogSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.dogSelectionSpinner)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.reminderNotes)
        val dateButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.datePickerButton)
        val timeButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.timePickerButton)

        // Setup reminder type spinner
        val reminderTypes = ReminderType.values().map { "${it.emoji} ${it.displayName}" }
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, reminderTypes)
        typeSpinner.setAdapter(typeAdapter)
        typeSpinner.setText(reminderTypes[0], false)

        // Setup dog selection spinner
        val dogNames = dogsList.map { "${it.first.name}" }
        val dogAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, dogNames)
        dogSpinner.setAdapter(dogAdapter)
        if (dogNames.isNotEmpty()) {
            dogSpinner.setText(dogNames[0], false)
        }

        // Pre-fill date if a date was selected in calendar
        selectedCalendarDate?.let { date ->
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
            selectedDate = date.format(formatter)
            dateButton.text = selectedDate
        }

        // Reset time
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
                val notes = notesInput.text.toString().trim()
                val selectedTypeText = typeSpinner.text.toString()
                val selectedDogText = dogSpinner.text.toString()

                if (title.isNotEmpty() && selectedDate.isNotEmpty() && selectedTime.isNotEmpty() && selectedDogText.isNotEmpty()) {
                    // Find selected reminder type
                    val reminderType = ReminderType.values().find {
                        "${it.emoji} ${it.displayName}" == selectedTypeText
                    } ?: ReminderType.OTHER

                    // Find selected dog
                    val selectedDog = dogsList.find { it.first.name == selectedDogText }

                    if (selectedDog != null) {
                        addReminder(title, reminderType, notes, selectedDate, selectedTime, selectedDog.second, selectedDog.first.name)
                    } else {
                        Toast.makeText(requireContext(), "Please select a valid dog", Toast.LENGTH_SHORT).show()
                    }
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
        val typeSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.reminderTypeSpinner)
        val dogSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.dogSelectionSpinner)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.reminderNotes)
        val dateButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.datePickerButton)
        val timeButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.timePickerButton)

        // Pre-fill with existing data
        titleInput.setText(reminder.title)
        notesInput.setText(reminder.notes)

        // Setup spinners
        val reminderTypes = ReminderType.values().map { "${it.emoji} ${it.displayName}" }
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, reminderTypes)
        typeSpinner.setAdapter(typeAdapter)
        typeSpinner.setText("${reminder.reminderType.emoji} ${reminder.reminderType.displayName}", false)

        val dogNames = dogsList.map { it.first.name }
        val dogAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, dogNames)
        dogSpinner.setAdapter(dogAdapter)
        dogSpinner.setText(reminder.dogName, false)

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
                val notes = notesInput.text.toString().trim()
                val selectedTypeText = typeSpinner.text.toString()
                val selectedDogText = dogSpinner.text.toString()

                if (title.isNotEmpty() && selectedDate.isNotEmpty() && selectedTime.isNotEmpty() && selectedDogText.isNotEmpty()) {
                    val reminderType = ReminderType.values().find {
                        "${it.emoji} ${it.displayName}" == selectedTypeText
                    } ?: ReminderType.OTHER

                    val selectedDog = dogsList.find { it.first.name == selectedDogText }

                    if (selectedDog != null) {
                        updateReminder(reminder, title, reminderType, notes, selectedDate, selectedTime, selectedDog.second, selectedDog.first.name)
                    }
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

    private fun addReminder(title: String, reminderType: ReminderType, notes: String, date: String, time: String, dogId: String, dogName: String) {
        val userId = auth.currentUser?.uid ?: return

        // Parse date and time to create timestamp
        val dateTimeString = "$date $time"
        val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val dateTime = dateTimeFormat.parse(dateTimeString)?.time ?: System.currentTimeMillis()

        val reminder = Reminder(
            id = UUID.randomUUID().toString(),
            title = title,
            reminderType = reminderType,
            dateTime = dateTime,
            notes = notes,
            dogId = dogId,
            dogName = dogName,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )

        saveReminderToFirebase(userId, reminder)
    }

    private fun updateReminder(reminder: Reminder, newTitle: String, reminderType: ReminderType, newNotes: String, newDate: String, newTime: String, dogId: String, dogName: String) {
        val userId = auth.currentUser?.uid ?: return

        // Parse date and time to create timestamp
        val dateTimeString = "$newDate $newTime"
        val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val dateTime = dateTimeFormat.parse(dateTimeString)?.time ?: reminder.dateTime

        val updatedReminder = reminder.copy(
            title = newTitle,
            reminderType = reminderType,
            notes = newNotes,
            dateTime = dateTime,
            dogId = dogId,
            dogName = dogName
        )

        updateReminderInFirebase(userId, updatedReminder)
    }

    private fun deleteReminder(reminder: Reminder) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Check if fragment is still attached before showing toast
            if (isAdded) {
                Toast.makeText(requireContext(), "Please log in to delete reminders", Toast.LENGTH_SHORT).show()
            }
            return
        }
        deleteReminderFromFirebase(userId, reminder)
    }

    private fun saveReminderToFirebase(userId: String, reminder: Reminder) {
        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminder.id)
            .set(reminder)
            .addOnSuccessListener {
                // Check if fragment is still attached before showing toast
                if (isAdded) {
                    Toast.makeText(requireContext(), "Reminder saved successfully", Toast.LENGTH_SHORT).show()
                }

                loadReminders()
            }
            .addOnFailureListener { exception ->
                // Check if fragment is still attached before showing toast
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to save reminder: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun updateReminderInFirebase(userId: String, reminder: Reminder) {
        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminder.id)
            .set(reminder)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Reminder updated", Toast.LENGTH_SHORT).show()
                }

                loadReminders()
            }
            .addOnFailureListener { exception ->
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to update reminder: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    private fun deleteReminderFromFirebase(userId: String, reminder: Reminder) {
        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminder.id)
            .delete()
            .addOnSuccessListener {
                // Check if fragment is still attached before showing toast
                if (isAdded) {
                    Toast.makeText(requireContext(), "Reminder deleted successfully", Toast.LENGTH_SHORT).show()
                }

                loadReminders()
            }
            .addOnFailureListener { exception ->
                // Check if fragment is still attached before showing toast
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to delete reminder: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateEmptyState(isEmpty: Boolean = remindersList.isEmpty()) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.remindersRecyclerView.visibility = View.GONE
            binding.selectedDateLayout.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.remindersRecyclerView.visibility = View.VISIBLE
            if (selectedCalendarDate != null) {
                binding.selectedDateLayout.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}