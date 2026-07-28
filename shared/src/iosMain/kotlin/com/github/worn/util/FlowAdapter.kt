package com.github.worn.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Cancellable(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

/**
 * Bridges a [StateFlow] of MVI state to Swift.
 *
 * Kotlin/Native exports this as an Objective-C lightweight generic, so Swift keeps the element
 * type by naming it explicitly — `FlowAdapter<WardrobeState>(flow: vm.state)`. It cannot be
 * inferred from the argument, because `StateFlow` itself is exported as a plain non-generic
 * protocol whose `value` is `Any?`.
 */
class FlowAdapter<T : Any>(private val flow: StateFlow<T>) {
    val currentValue: T get() = flow.value

    fun subscribe(onEach: (T) -> Unit): Cancellable {
        val job = CoroutineScope(Dispatchers.Main).launch {
            flow.collect { onEach(it) }
        }
        return Cancellable(job)
    }
}

/**
 * Bridges a one-shot effect [Flow] to Swift.
 *
 * Effects are a plain `Flow`, not a `StateFlow`, so they cannot go through [FlowAdapter]. The
 * element type stays `Any` rather than a generic parameter because the effect types are sealed
 * *interfaces*, which reach Swift as protocols and are awkward to name as a generic argument.
 * `Flow<T>` is covariant, so any `Flow<SomeEffect>` satisfies this constructor and Swift
 * recovers the concrete case with an `as?` cast.
 */
class EffectAdapter(private val flow: Flow<Any>) {
    fun subscribe(onEach: (Any) -> Unit): Cancellable {
        val job = CoroutineScope(Dispatchers.Main).launch {
            flow.collect { onEach(it) }
        }
        return Cancellable(job)
    }
}
