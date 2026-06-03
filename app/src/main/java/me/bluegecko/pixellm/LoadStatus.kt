package me.bluegecko.pixellm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LoadStatus {
    enum class Status {
        LOADING,
        HEALTHY,
        FAILED
    }

    private val _status = MutableStateFlow(Status.LOADING)
    val status: StateFlow<Status> = _status

    fun set(newStatus: Status) {
        _status.value = newStatus
    }
}