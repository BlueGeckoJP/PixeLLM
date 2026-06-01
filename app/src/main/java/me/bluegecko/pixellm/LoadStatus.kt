package me.bluegecko.pixellm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object LoadStatus {
    enum class Status {
        LOADING,
        HEALTHY,
        FAILED
    }

    private val mutex = Mutex()
    private val _status = MutableStateFlow(Status.LOADING)
    val status: StateFlow<Status> = _status

    suspend fun set(newStatus: Status) {
        mutex.withLock {
            _status.value = newStatus
        }
    }

    suspend fun get(): Status {
        return mutex.withLock { status.value }
    }
}