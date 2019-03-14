/*
 * Copyright 2019 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.tenkiv.kuantify.android.device

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.android.gate.*
import org.tenkiv.kuantify.android.gate.acquire.*
import org.tenkiv.kuantify.android.gate.control.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.communication.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.quantity.*

interface AndroidDevice : FSDevice {

    val ambientTemperatureSensors: List<AndroidQuantityInput<Temperature>>

    val heartRateSensors: List<AndroidQuantityInput<Frequency>>

    val lightSensors: List<AndroidQuantityInput<Illuminance>>

    val proximitySensors: List<AndroidQuantityInput<Length>>

    val pressureSensors: List<AndroidQuantityInput<Pressure>>

    val relativeHumiditySensors: List<AndroidQuantityInput<Dimensionless>>

    val torchControllers: List<AndroidOutput<BinaryState>>

    @Serializable
    data class Info(
        val deviceId: String,
        val numAmbientTemperatureSensors: Int,
        val numHeartRateSensors: Int,
        val numLightSensors: Int,
        val numProximitySensors: Int,
        val numPressureSensors: Int,
        val numRelativeHumiditySensors: Int,
        val numTorchControllers: Int
    )
}

class RemoteAndroidDevice internal constructor(
    scope: CoroutineScope,
    override val hostIp: String,
    override val uid: String,
    override val ambientTemperatureSensors: List<AndroidRemoteQuantityInput<Temperature>>,
    override val heartRateSensors: List<AndroidRemoteQuantityInput<Frequency>>,
    override val lightSensors: List<AndroidRemoteQuantityInput<Illuminance>>,
    override val pressureSensors: List<AndroidRemoteQuantityInput<Pressure>>,
    override val proximitySensors: List<AndroidRemoteQuantityInput<Length>>,
    override val relativeHumiditySensors: List<AndroidRemoteQuantityInput<Dimensionless>>,
    override val torchControllers: List<AndroidRemoteBinaryStateOutput>
) : FSRemoteDevice(scope.coroutineContext), AndroidDevice {

    protected override var networkCommunicator: FSRemoteNetworkCommunictor by networkCommunicator()

    override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)
        ambientTemperatureSensors.addSideRoutingTo(routing)
        heartRateSensors.addSideRoutingTo(routing)
        lightSensors.addSideRoutingTo(routing)
        pressureSensors.addSideRoutingTo(routing)
        proximitySensors.addSideRoutingTo(routing)
        relativeHumiditySensors.addSideRoutingTo(routing)
        torchControllers.addSideRoutingTo(routing)
    }

    override fun toString(): String {
        return "RemoteAndroidDevice(uid=$uid, hostIp=$hostIp, job=${coroutineContext[Job]})"
    }
}

//TODO: Error handling
suspend fun CoroutineScope.RemoteAndroidDeivce(hostIp: String): RemoteAndroidDevice {
    val info = Json.parse(AndroidDevice.Info.serializer(), FSRemoteDevice.getInfo(hostIp))

    val ambientTemperatureSensors = ArrayList<AndroidRemoteQuantityInput<Temperature>>()
    for (i in 0 until info.numAmbientTemperatureSensors) {
        ambientTemperatureSensors += AndroidRemoteQuantityInput(
            this,
            "${AndroidGateTypeId.AMBIENT_TEMPERATURE}$i",
            Temperature::class
        )
    }

    val heartRateSensors = ArrayList<AndroidRemoteQuantityInput<Frequency>>()
    for (i in 0 until info.numHeartRateSensors) {
        heartRateSensors += AndroidRemoteQuantityInput(
            this,
            "${AndroidGateTypeId.HEART_RATE}$i",
            Frequency::class
        )
    }

    val lightSensors = ArrayList<AndroidRemoteQuantityInput<Illuminance>>()
    for (i in 0 until info.numLightSensors) {
        lightSensors += AndroidRemoteQuantityInput(
            this,
            "${AndroidGateTypeId.LIGHT}$i",
            Illuminance::class
        )
    }

    val pressureSensors = ArrayList<AndroidRemoteQuantityInput<Pressure>>()
    for (i in 0 until info.numPressureSensors) {
        pressureSensors += AndroidRemoteQuantityInput(
            this,
            "${AndroidGateTypeId.PRESSURE}$i",
            Pressure::class
        )
    }

    val proximitySensors = ArrayList<AndroidRemoteQuantityInput<Length>>()
    for (i in 0 until info.numProximitySensors) {
        proximitySensors += AndroidRemoteQuantityInput(
            this,
            "${AndroidGateTypeId.PROXIMITY}$i",
            Length::class
        )
    }

    val relativeHumiditySensors = ArrayList<AndroidRemoteQuantityInput<Dimensionless>>()
    for (i in 0 until info.numRelativeHumiditySensors) {
        relativeHumiditySensors += AndroidRemoteQuantityInput(
            this,
            "${AndroidGateTypeId.RELATIVE_HUMIDITY}$i",
            Dimensionless::class
        )
    }

    val torchControllers = ArrayList<AndroidRemoteBinaryStateOutput>()
    for (i in 0 until info.numTorchControllers) {
        torchControllers += AndroidRemoteBinaryStateOutput(
            this,
            "${AndroidGateTypeId.TORCH}$i"
        )
    }


    return RemoteAndroidDevice(
        this,
        hostIp,
        info.deviceId,
        ambientTemperatureSensors,
        heartRateSensors,
        lightSensors,
        pressureSensors,
        proximitySensors,
        relativeHumiditySensors,
        torchControllers
    )
}