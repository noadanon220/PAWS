package com.danono.paws.ui.reminders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.danono.paws.databinding.FragmentRemindersBinding


class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val remindersViewModel =
            ViewModelProvider(this).get(RemindersViewModel::class.java)

        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textReminders
        remindersViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}