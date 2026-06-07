package me.bluegecko.pixellm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.bluegecko.pixellm.model.LoadStatus

object LoadStatusStore {
    private val _status = MutableStateFlow(LoadStatus.UNLOADED)
    val status: StateFlow<LoadStatus> = _status

    fun set(newStatus: LoadStatus) {
        _status.value = newStatus
    }
}