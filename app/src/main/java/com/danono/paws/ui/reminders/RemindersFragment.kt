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
import com.danono.paws.model.Dog
import com.danono.paws.model.Reminder
import com.danono.paws.model.ReminderType
import com.danono.paws.ui.mydogs.SharedDogsViewModel
import com.danono.paws.utilities.FirebaseDataManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.ListenerRegistration
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID


class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private lateinit var remindersAdapter: RemindersAdapter
    private lateinit var remindersViewModel: RemindersViewModel
    private lateinit var sharedDogsViewModel: SharedDogsViewModel

    private val remindersList = mutableListOf<Reminder>()       // full list from listener
    private val dogsList = mutableListOf<Pair<Dog, String>>()   // (Dog, docId) for dog selector

    private var remindersListener: ListenerRegistration? = null

    // Calendar state
    private var selectedDate = ""
    private var selectedTime = ""
    private var selectedCalendarDate: LocalDate? = null

    private val scope = MainScope()

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

    // -------------------- Lifecycle --------------------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendar()
        setupRecyclerView()
        setupFab()
        loadDogs()
        attachRemindersListener()
        setupMonthNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Ensure a date is always selected; default to today
        if (selectedCalendarDate == null) selectedCalendarDate = LocalDate.now()
        applyFilterAndRender()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        remindersListener?.remove()
        remindersListener = null
        _binding = null
    }

    // -------------------- Calendar --------------------

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)

        binding.calendarView.setup(startMonth, endMonth, java.time.DayOfWeek.SUNDAY)
        binding.calendarView.scrollToMonth(currentMonth)

        updateMonthYearText(currentMonth)

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: com.kizitonwose.calendar.core.CalendarDay) {
                container.bind(data)
            }
        }

        binding.calendarView.monthScrollListener = { calendarMonth ->
            updateMonthYearText(calendarMonth.yearMonth)
        }

        // Select "today" by default after layout
        binding.calendarView.post {
            if (selectedCalendarDate == null) {
                selectDate(LocalDate.now())
            } else {
                selectDate(selectedCalendarDate!!)
            }
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

        fun bind(day: com.kizitonwose.calendar.core.CalendarDay) {
            dayText.text = day.date.dayOfMonth.toString()

            // Reset styling
            dayText.setTextColor(resources.getColor(R.color.black, null))
            view.background = null
            indicator.visibility = View.GONE

            when (day.position) {
                DayPosition.MonthDate -> {
                    dayText.visibility = View.VISIBLE

                    val hasReminders = remindersList.any { r ->
                        millisToLocalDate(r.dateTime) == day.date
                    }
                    if (hasReminders) indicator.visibility = View.VISIBLE

                    if (day.date == selectedCalendarDate) {
                        view.setBackgroundResource(R.drawable.bg_circle_selected)
                        dayText.setTextColor(resources.getColor(R.color.white, null))
                    }

                    if (day.date == LocalDate.now() && day.date != selectedCalendarDate) {
                        dayText.setTextColor(resources.getColor(R.color.Primary_pink, null))
                    }

                    view.setOnClickListener {
                        selectDate(day.date)
                    }
                }
                DayPosition.InDate, DayPosition.OutDate -> {
                    dayText.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun selectDate(date: LocalDate) {
        selectedCalendarDate = date
        binding.calendarView.notifyCalendarChanged()

        binding.selectedDateLayout.visibility = View.VISIBLE
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
        binding.selectedDateText.text = date.format(formatter)

        applyFilterAndRender()
    }

    // -------------------- RecyclerView --------------------

    private fun setupRecyclerView() {
        remindersAdapter = RemindersAdapter(emptyList()) { reminder ->
            showEditReminderDialog(reminder)
        }
        binding.remindersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = remindersAdapter
        }
    }

    private fun applyFilterAndRender() {
        val targetDate = selectedCalendarDate ?: LocalDate.now()
        val dateReminders = remindersList.filter { millisToLocalDate(it.dateTime) == targetDate }

        binding.reminderCountText.text = "${dateReminders.size} reminders"

        if (::remindersAdapter.isInitialized) {
            remindersAdapter.submit(dateReminders)
        } else {
            remindersAdapter = RemindersAdapter(dateReminders) { reminder ->
                showEditReminderDialog(reminder)
            }
            binding.remindersRecyclerView.adapter = remindersAdapter
        }

        updateEmptyState(dateReminders.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.remindersRecyclerView.visibility = View.GONE
            binding.selectedDateLayout.visibility = View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.remindersRecyclerView.visibility = View.VISIBLE
            binding.selectedDateLayout.visibility = View.VISIBLE
        }
    }

    // -------------------- Data: dogs + reminders --------------------

    private fun loadDogs() {
        // Load dogs using manager for the dog selection spinner in dialogs
        scope.launch {
            val res = FirebaseDataManager.getInstance().getDogs()
            dogsList.clear()
            dogsList.addAll(res.getOrNull().orEmpty())
        }
    }

    private fun attachRemindersListener() {
        // Real-time updates from root-level reminders
        remindersListener?.remove()
        remindersListener = FirebaseDataManager.getInstance().addRemindersListener { all ->
            remindersList.clear()
            remindersList.addAll(all) // already sorted asc by dateTime
            binding.calendarView.notifyCalendarChanged()
            applyFilterAndRender()
            Log.d("RemindersFragment", "Listener received ${all.size} reminders")
        }
    }

    // -------------------- FAB + Dialogs --------------------

    private fun setupFab() {
        binding.fabAddReminder.setOnClickListener {
            showAddReminderDialog()
        }
    }

    private fun showAddReminderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)

        val typeSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.reminderTypeSpinner)
        val dogSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.dogSelectionSpinner)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.reminderNotes)
        val dateButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.datePickerButton)
        val timeButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.timePickerButton)
        val locationInput = dialogView.findViewById<TextInputEditText?>(R.id.reminderLocation)

        val types = ReminderType.values().toList()
        val typeLabels = types.map { "${it.emoji} ${it.displayName}" }
        typeSpinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, typeLabels))
        typeSpinner.setText(typeLabels.first(), false)
        var selectedType = types.first()
        typeSpinner.setOnItemClickListener { _, _, pos, _ -> selectedType = types[pos] }

        val dogNames = dogsList.map { it.first.name }
        val dogAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, dogNames)
        dogSpinner.setAdapter(dogAdapter)
        if (dogNames.isNotEmpty()) dogSpinner.setText(dogNames[0], false)

        // Prefill date from selected calendar date or today
        selectedCalendarDate?.let { date ->
            val df = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
            selectedDate = date.format(df)
        } ?: run {
            val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            selectedDate = df.format(Date())
        }
        dateButton.text = selectedDate

        // Default time
        selectedTime = "09:00"
        timeButton.text = selectedTime

        dateButton.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                dateButton.text = date
            }
        }
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
                val notes = notesInput.text?.toString()?.trim().orEmpty()
                val selectedDogText = dogSpinner.text?.toString()?.trim().orEmpty()
                val location = locationInput?.text?.toString()?.trim().orEmpty()

                if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty() && selectedDogText.isNotEmpty()) {
                    val selectedDog = dogsList.find { it.first.name == selectedDogText }
                    if (selectedDog != null) {
                        addReminder(
                            title = selectedType.displayName,
                            reminderType = selectedType,
                            notes = notes,
                            date = selectedDate,
                            time = selectedTime,
                            dogId = selectedDog.second,
                            dogName = selectedDog.first.name,
                            location = location
                        )
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

        val typeSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.reminderTypeSpinner)
        val dogSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.dogSelectionSpinner)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.reminderNotes)
        val dateButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.datePickerButton)
        val timeButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.timePickerButton)
        val locationInput = dialogView.findViewById<TextInputEditText?>(R.id.reminderLocation)

        // Prefill
        notesInput.setText(reminder.notes)
        locationInput?.setText(reminder.location)

        val types = ReminderType.values().toList()
        val typeLabels = types.map { "${it.emoji} ${it.displayName}" }
        typeSpinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, typeLabels))
        typeSpinner.setText("${reminder.reminderType.emoji} ${reminder.reminderType.displayName}", false)
        var selectedType = reminder.reminderType
        typeSpinner.setOnItemClickListener { _, _, pos, _ -> selectedType = types[pos] }

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

        dateButton.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                dateButton.text = date
            }
        }
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
                val notes = notesInput.text?.toString()?.trim().orEmpty()
                val selectedDogText = dogSpinner.text?.toString()?.trim().orEmpty()
                val location = locationInput?.text?.toString()?.trim().orEmpty()

                val selectedDog = dogsList.find { it.first.name == selectedDogText }
                if (selectedDog != null) {
                    updateReminder(
                        reminder = reminder,
                        newTitle = selectedType.displayName,
                        reminderType = selectedType,
                        newNotes = notes,
                        newDate = selectedDate,
                        newTime = selectedTime,
                        dogId = selectedDog.second,
                        dogName = selectedDog.first.name,
                        location = location
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ -> deleteReminder(reminder) }
            .show()
    }

    // -------------------- Pickers --------------------

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        selectedCalendarDate?.let { d ->
            cal.set(d.year, d.monthValue - 1, d.dayOfMonth)
        }
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val c = Calendar.getInstance().apply { set(y, m, d) }
                val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                onDateSelected(df.format(c.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        val hour = if (selectedTime.isNotEmpty()) selectedTime.substring(0, 2).toInt() else cal.get(Calendar.HOUR_OF_DAY)
        val minute = if (selectedTime.isNotEmpty()) selectedTime.substring(3, 5).toInt() else cal.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _, h, m -> onTimeSelected(String.format("%02d:%02d", h, m)) },
            hour,
            minute,
            true
        ).show()
    }

    // -------------------- CRUD helpers (root collection) --------------------

    private fun addReminder(
        title: String,
        reminderType: ReminderType,
        notes: String,
        date: String,
        time: String,
        dogId: String,
        dogName: String,
        location: String = ""
    ) {
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
            location = location,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )

        scope.launch {
            val res = FirebaseDataManager.getInstance().saveReminderToRoot(reminder)
            if (!res.isSuccess && isAdded) {
                Toast.makeText(requireContext(), "Failed to save reminder", Toast.LENGTH_SHORT).show()
            }
            // Listener will refresh UI automatically
        }
    }

    private fun updateReminder(
        reminder: Reminder,
        newTitle: String,
        reminderType: ReminderType,
        newNotes: String,
        newDate: String,
        newTime: String,
        dogId: String,
        dogName: String,
        location: String = ""
    ) {
        val dateTimeString = "$newDate $newTime"
        val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val dateTime = dateTimeFormat.parse(dateTimeString)?.time ?: reminder.dateTime

        val updatedReminder = reminder.copy(
            title = newTitle,
            reminderType = reminderType,
            notes = newNotes,
            dateTime = dateTime,
            dogId = dogId,
            dogName = dogName,
            location = location
        )

        scope.launch {
            val res = FirebaseDataManager.getInstance().updateReminderInRoot(updatedReminder)
            if (!res.isSuccess && isAdded) {
                Toast.makeText(requireContext(), "Failed to update reminder", Toast.LENGTH_SHORT).show()
            }
            // Listener will refresh UI automatically
        }
    }

    private fun deleteReminder(reminder: Reminder) {
        scope.launch {
            val res = FirebaseDataManager.getInstance().deleteReminderFromRoot(reminder.id)
            if (!res.isSuccess && isAdded) {
                Toast.makeText(requireContext(), "Failed to delete reminder", Toast.LENGTH_SHORT).show()
            }
            // Listener will refresh UI automatically
        }
    }

    // -------------------- Utils --------------------

    private fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}