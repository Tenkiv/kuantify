package org.tenkiv.kuantify.android

import kotlinx.coroutines.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import javax.measure.*
import kotlin.coroutines.*
import kotlin.reflect.*

typealias QuantityAndroidSensor<Q> = AndroidSensor<DaqcQuantity<Q>>

interface AndroidSensor<T : DaqcValue> : Input<T> {
    val uid: String
}

class RemoteQuantityAndroidSensor<Q : Quantity<Q>>(
    private val scope: CoroutineScope,
    override val uid: String,
    override val quantityType: KClass<Q>
) : FSRemoteQuantityInput<Q>(), QuantityAndroidSensor<Q> {

    override val coroutineContext: CoroutineContext get() = scope.coroutineContext

    override val updateRate: UpdateRate by runningAverage()
}

class RemoteBinaryStateAndroidSensor(
    private val scope: CoroutineScope,
    override val uid: String
) : FSRemoteBinaryStateInput(), AndroidSensor<BinaryState> {

    override val coroutineContext: CoroutineContext get() = scope.coroutineContext

    override val updateRate: UpdateRate by runningAverage()

}

object AndroidSensorTypeId {
    const val AMBIENT_TEMPERATURE = "AT"
    const val HEART_RATE = "HR"
    const val LIGHT = "LI"
    const val PROXIMITY = "PX"
    const val PRESSURE = "PS"
    const val RELATIVE_HUMIDITY = "HU"
}