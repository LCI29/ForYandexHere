package ru.clementl.metrotimex.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.clementl.metrotimex.model.data.MachinistStatus
import ru.clementl.metrotimex.repositories.CalendarRepository
import ru.clementl.metrotimex.repositories.MachinistStatusRepository
import ru.clementl.metrotimex.utils.logd
import java.lang.IllegalStateException

class StatusViewModel(
    val machinistStatusRepository: MachinistStatusRepository
) : ViewModel() {

    val liveStatusList: LiveData<List<MachinistStatus>> = machinistStatusRepository.getAllAsLiveData()



}

class StatusViewModelFactory(
    private val machinistStatusRepository: MachinistStatusRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatusViewModel(machinistStatusRepository) as T
        }
        throw IllegalStateException("Unknown ViewModel class")
    }
}