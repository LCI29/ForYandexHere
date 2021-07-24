package ru.clementl.metrotimex.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import ru.clementl.metrotimex.MetroTimeApplication
import ru.clementl.metrotimex.R
import ru.clementl.metrotimex.converters.toInt
import ru.clementl.metrotimex.converters.toLong
import ru.clementl.metrotimex.converters.toStringCode
import ru.clementl.metrotimex.databinding.FragmentShiftCreateBinding
import ru.clementl.metrotimex.model.data.DayStatus
import ru.clementl.metrotimex.model.data.Shift
import ru.clementl.metrotimex.model.data.WorkDayType
import ru.clementl.metrotimex.model.data.weekDayType
import ru.clementl.metrotimex.ui.fragments.pickers.DatePickerFragment
import ru.clementl.metrotimex.ui.fragments.pickers.TimePickerFragment
import ru.clementl.metrotimex.utils.*
import ru.clementl.metrotimex.viewmodel.CalendarViewModel
import ru.clementl.metrotimex.viewmodel.SharedViewModel
import ru.clementl.metrotimex.viewmodel.ShiftCreateViewModel
import ru.clementl.metrotimex.viewmodel.ShiftCreateViewModelFactory
import java.lang.Exception

/**
 * Shift create OR edit fragment
 */

class ShiftCreateFragment : Fragment(), AdapterView.OnItemSelectedListener {


    private var binding: FragmentShiftCreateBinding? = null
    private lateinit var spinner: Spinner
    private lateinit var shiftCreateViewModel: ShiftCreateViewModel
    private lateinit var arguments: ShiftCreateFragmentArgs
    val calendarViewModel: CalendarViewModel by activityViewModels()
    val sharedViewModel: SharedViewModel by activityViewModels()




    companion object {
        const val DATE_PICKER = "date_picker"
        const val TIME_PICKER_START = "time_picker_start"
        const val TIME_PICKER_END = "time_picker_end"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentBinding = FragmentShiftCreateBinding.inflate(inflater, container, false)
        binding = fragmentBinding

        val application = requireNotNull(activity).application
        arguments = ShiftCreateFragmentArgs.fromBundle(requireArguments())

        val dataSource = (application as MetroTimeApplication).repository
        val viewModelFactory = ShiftCreateViewModelFactory(
            dataSource, arguments.mode, sharedViewModel.currentDay)

        shiftCreateViewModel =
            ViewModelProvider(
                this, viewModelFactory).get(ShiftCreateViewModel::class.java)
        binding!!.viewModel = shiftCreateViewModel

        shiftCreateViewModel.workDayTypeLive.observe(viewLifecycleOwner, {
            spinner.setSelection(it.toInt())
        })


        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // make spinner
        spinner = binding!!.spDayType
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.day_status_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = this

        with(binding!!) {
            buttonStartTime.setOnClickListener {
                logd("on button current day${shiftCreateViewModel.editingDay?.date?.asSimpleDate()}")
                TimePickerFragment(shiftCreateViewModel).show(requireActivity().supportFragmentManager, TIME_PICKER_START)
            }

            buttonEndTime.setOnClickListener {
                TimePickerFragment(shiftCreateViewModel).show(requireActivity().supportFragmentManager, TIME_PICKER_END)
            }

            fieldChooseDate.setOnClickListener {
                DatePickerFragment(shiftCreateViewModel)
                    .show(requireActivity().supportFragmentManager, DATE_PICKER)
            }
            cancelButton.setOnClickListener {
//                findNavController().navigate(R.id.action_shiftEditDialogFragment_to_calendarFragment)
                findNavController().navigateUp()
            }
        }

        with (shiftCreateViewModel) {
            initializeStartDate()
            startDate.observe(viewLifecycleOwner) {
                binding!!.fieldChooseDate.text = it.asSimpleDate()
            }
            startTime.observe(viewLifecycleOwner) {
                binding!!.buttonStartTime.text = it.asSimpleTime()
            }
            endTime.observe(viewLifecycleOwner) {
                binding!!.buttonEndTime.text = it.asSimpleTime()
            }
        }

        binding!!.saveButton.setOnClickListener { saveDay() }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, p3: Long) {
        when (pos) {
            0 -> {
                showToast("0")
                shiftCreateViewModel.workDayType = WorkDayType.SHIFT
                with(binding!!) {
                    etShiftName.visibility = View.VISIBLE
                    tvStartText.visibility = View.VISIBLE
                    tvEndText.visibility = View.VISIBLE
                    buttonStartTime.visibility = View.VISIBLE
                    buttonEndTime.visibility = View.VISIBLE
                    etStartPlace.visibility = View.VISIBLE
                    etEndPlace.visibility = View.VISIBLE
                }
            }
            else -> {
                with(shiftCreateViewModel) {
                    showToast(pos.toString())
                    when (pos) {
                        1 -> workDayType = WorkDayType.WEEKEND
                        2 -> workDayType = WorkDayType.SICK_LIST
                        3 -> workDayType = WorkDayType.VACATION_DAY
                        4 -> workDayType = WorkDayType.MEDIC_DAY

                    }
                }
                with(binding!!) {
                    etShiftName.visibility = View.GONE
                    tvStartText.visibility = View.GONE
                    tvEndText.visibility = View.GONE
                    buttonStartTime.visibility = View.GONE
                    buttonEndTime.visibility = View.GONE
                    etStartPlace.visibility = View.GONE
                    etEndPlace.visibility = View.GONE
                }
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }

    override fun onDestroy() {
        super.onDestroy()
        shiftCreateViewModel.reset()
    }

    /**
     * Saves day and shift in db.
     */
    private fun saveDay() {
        val binding = binding!!
        val date = shiftCreateViewModel.startDate.value ?: throw Exception("Date no set")
        val startTime = shiftCreateViewModel.startTime.value ?: throw Exception("Start time not set")
        val endTime = shiftCreateViewModel.endTime.value ?: throw Exception("End time not set")
        val shift = if (shiftCreateViewModel.workDayType == WorkDayType.SHIFT) {
            Shift(
                name = binding.etShiftName.text.toString(),
                weekDayTypeString = date.weekDayType().toStringCode(),
                oddEven = date.oddEven(endTime),
                startTimeInt = startTime.toInt(),
                startLoc = binding.etStartPlace.text.toString(),
                endTimeInt = endTime.toInt(),
                endLoc = binding.etEndPlace.text.toString()
            )
        } else {
            null
        }

        val day = DayStatus(date.toLong(), shiftCreateViewModel.workDayType.toInt(), shift)
        calendarViewModel.insert(day)
        findNavController().navigate(R.id.action_shiftEditDialogFragment_to_calendarFragment)
    }

}