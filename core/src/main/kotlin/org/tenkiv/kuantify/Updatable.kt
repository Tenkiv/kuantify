package org.tenkiv.kuantify

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import tec.units.indriya.*
import kotlin.coroutines.*

typealias UpdatableQuantity<Q> = Updatable<ComparableQuantity<Q>>
typealias InitializedUpdatableQuantity<Q> = InitializedUpdatable<ComparableQuantity<Q>>

/**
 * Same as [Trackable] but allows setting.
 */
interface Updatable<T> : Trackable<T> {

    override val updateBroadcaster: ConflatedBroadcastChannel<T>

}

interface InitializedUpdatable<T> : Updatable<T>, InitializedTrackable<T> {

    override var value: T

}

fun <T> Updatable<T>.set(value: T) {
    updateBroadcaster.offer(value)
}

private class SimpleUpdatable<T>(scope: CoroutineScope) : Updatable<T> {

    override val coroutineContext: CoroutineContext = scope.coroutineContext

    override val updateBroadcaster: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel()

}

private class SimpleInitializedUpdatable<T>(scope: CoroutineScope, initialValue: T) : InitializedUpdatable<T> {

    override val coroutineContext: CoroutineContext = scope.coroutineContext

    override val updateBroadcaster: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel(initialValue)

    override var value: T
        get() = updateBroadcaster.value
        set(value) = set(value)
}

fun <T> CoroutineScope.Updatable(): Updatable<T> = SimpleUpdatable(this)

fun <T> CoroutineScope.Updatable(initialValue: T): InitializedUpdatable<T> =
    SimpleInitializedUpdatable(this, initialValue)