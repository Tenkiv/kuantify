package org.tenkiv.kuantify

import kotlinx.coroutines.channels.*

/**
 * Same as [Trackable] but allows setting.
 */
interface Updatable<T> : Trackable<T> {

    override val updateBroadcaster: ConflatedBroadcastChannel<T>

}

suspend fun <T> Updatable<T>.set(value: T): Unit = updateBroadcaster.send(value)