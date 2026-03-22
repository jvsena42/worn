package com.github.worn.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Cancellable(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

class FlowAdapter<T : Any>(private val flow: StateFlow<T>) {
    val currentValue: T get() = flow.value

    fun subscribe(onEach: (T) -> Unit): Cancellable {
        val job = CoroutineScope(Dispatchers.Main).launch {
            flow.collect { onEach(it) }
        }
        return Cancellable(job)
    }
}
