package com.danono.paws.ui.mydogs

import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.CalendarDayAdapter
import com.danono.paws.databinding.FragmentDogWalksBinding
import com.danono.paws.model.CalendarDay
import com.danono.paws.model.WalkData
import java.text.SimpleDateFormat
import java.util.*

class DogWalksFragment : Fragment(R.layout.fragment_dog_walks) {

    private var _binding: FragmentDogWalksBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarDayAdapter
    private lateinit var preferences: SharedPreferences

    private val days = mutableListOf<CalendarDay>()
    private var selectedDate: String = ""
    private var currentCalendar = Calendar.getInstance()

    // Default walk times
    private var morningTime = "08:00"
    private var afternoonTime = "14:00"
    private var eveningTime = "20:00"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDogWalksBinding.bind(view)

        preferences = requireContext().getSharedPreferences("walk_preferences", Context.MODE_PRIVATE)

        // Load saved times
        loadSavedTimes()

        // Setup UI components
        setupCalendar()
        setupTimeEditing()
        setupWalkTracking()

        // Initialize with current date
        selectedDate = getCurrentDateString()
        loadCurrentWeek()
        updateSelectedDate()
    }

    /**
     * Setup the horizontal calendar RecyclerView
     */
    private fun setupCalendar() {
        calendarAdapter = CalendarDayAdapter(days) { selectedDay ->
            selectedDate = selectedDay.date
            updateSelectedDate()
            updateWalkTrackingForDate()
        }

        binding.weekCalendarRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = calendarAdapter
        }

        // Month navigation
        binding.previousMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateMonthHeader()
            loadCurrentWeek()
        }

        binding.nextMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateMonthHeader()
            loadCurrentWeek()
        }
    }

    /**
     * Setup time editing functionality
     */
    private fun setupTimeEditing() {
        // Update display
        binding.morningTimeText.text = formatTimeDisplay(morningTime)
        binding.afternoonTimeText.text = formatTimeDisplay(afternoonTime)
        binding.eveningTimeText.text = formatTimeDisplay(eveningTime)

        // Edit icon click listeners - NEW
        binding.editMorningTimeButton.setOnClickListener {
            showTimePickerDialog(morningTime) { newTime ->
                morningTime = newTime
                binding.morningTimeText.text = formatTimeDisplay(newTime)
                binding.morningWalkTimeDisplay.text = formatTimeDisplay(newTime)
                saveTime("morning_time", newTime)
            }
        }

        binding.editAfternoonTimeButton.setOnClickListener {
            showTimePickerDialog(afternoonTime) { newTime ->
                afternoonTime = newTime
                binding.afternoonTimeText.text = formatTimeDisplay(newTime)
                binding.afternoonWalkTimeDisplay.text = formatTimeDisplay(newTime)
                saveTime("afternoon_time", newTime)
            }
        }

        binding.editEveningTimeButton.setOnClickListener {
            showTimePickerDialog(eveningTime) { newTime ->
                eveningTime = newTime
                binding.eveningTimeText.text = formatTimeDisplay(newTime)
                binding.eveningWalkTimeDisplay.text = formatTimeDisplay(newTime)
                saveTime("evening_time", newTime)
            }
        }

        // Text click listeners (optional - keep if you want text to also be clickable)
        binding.morningTimeText.setOnClickListener {
            showTimePickerDialog(morningTime) { newTime ->
                morningTime = newTime
                binding.morningTimeText.text = formatTimeDisplay(newTime)
                binding.morningWalkTimeDisplay.text = formatTimeDisplay(newTime)
                saveTime("morning_time", newTime)
            }
        }

        binding.afternoonTimeText.setOnClickListener {
            showTimePickerDialog(afternoonTime) { newTime ->
                afternoonTime = newTime
                binding.afternoonTimeText.text = formatTimeDisplay(newTime)
                binding.afternoonWalkTimeDisplay.text = formatTimeDisplay(newTime)
                saveTime("afternoon_time", newTime)
            }
        }

        binding.eveningTimeText.setOnClickListener {
            showTimePickerDialog(eveningTime) { newTime ->
                eveningTime = newTime
                binding.eveningTimeText.text = formatTimeDisplay(newTime)
                binding.eveningWalkTimeDisplay.text = formatTimeDisplay(newTime)
                saveTime("evening_time", newTime)
            }
        }
    }

    /**
     * Setup walk tracking checkboxes
     */
    private fun setupWalkTracking() {
        // Update display times
        binding.morningWalkTimeDisplay.text = formatTimeDisplay(morningTime)
        binding.afternoonWalkTimeDisplay.text = formatTimeDisplay(afternoonTime)
        binding.eveningWalkTimeDisplay.text = formatTimeDisplay(eveningTime)

        binding.morningWalkCheckbox.setOnCheckedChangeListener { _, isChecked ->
            saveWalkCompletion(selectedDate, "morning", isChecked)
            updateCalendarIndicators()
        }

        binding.afternoonWalkCheckbox.setOnCheckedChangeListener { _, isChecked ->
            saveWalkCompletion(selectedDate, "afternoon", isChecked)
            updateCalendarIndicators()
        }

        binding.eveningWalkCheckbox.setOnCheckedChangeListener { _, isChecked ->
            saveWalkCompletion(selectedDate, "evening", isChecked)
            updateCalendarIndicators()
        }
    }

    /**
     * Show time picker dialog
     */
    private fun showTimePickerDialog(currentTime: String, onTimeSelected: (String) -> Unit) {
        val timeParts = currentTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(newTime)
        }, hour, minute, true).show()
    }

    /**
     * Load current week days into calendar
     */
    private fun loadCurrentWeek() {
        days.clear()

        val calendar = Calendar.getInstance()
        calendar.time = currentCalendar.time

        // Get start of week (Sunday)
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

        // Generate 7 days
        for (i in 0..6) {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val dayAbbreviation = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
            val dayNumber = calendar.get(Calendar.DAY_OF_MONTH)

            val isToday = dateString == getCurrentDateString()
            val isSelected = dateString == selectedDate

            // Get walk completion data for this date
            val walkData = getWalkDataForDate(dateString)

            days.add(CalendarDay(
                date = dateString,
                dayAbbreviation = dayAbbreviation,
                dayNumber = dayNumber,
                isToday = isToday,
                isSelected = isSelected,
                walkData = walkData
            ))

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        calendarAdapter.notifyDataSetChanged()
        updateMonthHeader()
    }

    /**
     * Update month/year header
     */
    private fun updateMonthHeader() {
        val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentCalendar.time)
        binding.monthYearText.text = monthYear
    }

    /**
     * Update selected date display
     */
    private fun updateSelectedDate() {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        calendar.time = sdf.parse(selectedDate) ?: Date()

        val displayFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val dateText = if (selectedDate == getCurrentDateString()) {
            "Today's Walks"
        } else {
            "${displayFormat.format(calendar.time)} Walks"
        }

        binding.selectedDateText.text = dateText

        // Update calendar selection
        days.forEach { it.isSelected = it.date == selectedDate }
        calendarAdapter.notifyDataSetChanged()
    }

    /**
     * Update walk tracking checkboxes for selected date
     */
    private fun updateWalkTrackingForDate() {
        val walkData = getWalkDataForDate(selectedDate)

        binding.morningWalkCheckbox.isChecked = walkData.morningCompleted
        binding.afternoonWalkCheckbox.isChecked = walkData.afternoonCompleted
        binding.eveningWalkCheckbox.isChecked = walkData.eveningCompleted
    }

    /**
     * Update calendar indicators for all days
     */
    private fun updateCalendarIndicators() {
        days.forEach { day ->
            day.walkData = getWalkDataForDate(day.date)
        }
        calendarAdapter.notifyDataSetChanged()
    }

    /**
     * Get walk completion data for a specific date
     */
    private fun getWalkDataForDate(date: String): WalkData {
        return WalkData(
            morningCompleted = preferences.getBoolean("${date}_morning", false),
            afternoonCompleted = preferences.getBoolean("${date}_afternoon", false),
            eveningCompleted = preferences.getBoolean("${date}_evening", false)
        )
    }

    /**
     * Save walk completion status
     */
    private fun saveWalkCompletion(date: String, period: String, completed: Boolean) {
        preferences.edit()
            .putBoolean("${date}_${period}", completed)
            .apply()
    }

    /**
     * Load saved walk times from preferences
     */
    private fun loadSavedTimes() {
        morningTime = preferences.getString("morning_time", "08:00") ?: "08:00"
        afternoonTime = preferences.getString("afternoon_time", "14:00") ?: "14:00"
        eveningTime = preferences.getString("evening_time", "20:00") ?: "20:00"
    }

    /**
     * Save walk time to preferences
     */
    private fun saveTime(key: String, time: String) {
        preferences.edit()
            .putString(key, time)
            .apply()
    }

    /**
     * Format time for display (24h to 12h format)
     */
    private fun formatTimeDisplay(time24h: String): String {
        val timeParts = time24h.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }

        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    /**
     * Get current date as string
     */
    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        updateWalkTrackingForDate()
        updateCalendarIndicators()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}