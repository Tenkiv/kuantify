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
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.quantity.*

public interface AndroidDevice : FSDevice {

    public val ambientTemperatureSensors: List<AndroidQuantityInput<Temperature>>

    public val heartRateSensors: List<AndroidQuantityInput<Frequency>>

    public val lightSensors: List<AndroidQuantityInput<Illuminance>>

    public val proximitySensors: List<AndroidQuantityInput<Length>>

    public val pressureSensors: List<AndroidQuantityInput<Pressure>>

    public val relativeHumiditySensors: List<AndroidQuantityInput<Dimensionless>>

    public val torchControllers: List<AndroidOutput<BinaryState>>

    @Serializable
    public data class Info(
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

public class RemoteAndroidDevice internal constructor(
    scope: CoroutineScope,
    public override val hostIp: String,
    info: AndroidDevice.Info
) : FSRemoteDevice(scope.coroutineContext), AndroidDevice {

    public override val uid: String = info.deviceId

    public override val ambientTemperatureSensors: List<AndroidRemoteQuantityInput<Temperature>> = run {
        val ambientTemperatureSensors = ArrayList<AndroidRemoteQuantityInput<Temperature>>()
        for (i in 0 until info.numAmbientTemperatureSensors) {
            ambientTemperatureSensors += AndroidRemoteQuantityInput(
                this,
                "${AndroidGateTypeId.AMBIENT_TEMPERATURE}$i",
                Temperature::class
            )
        }
        ambientTemperatureSensors
    }

    public override val heartRateSensors: List<AndroidRemoteQuantityInput<Frequency>> = run {
        val heartRateSensors = ArrayList<AndroidRemoteQuantityInput<Frequency>>()
        for (i in 0 until info.numHeartRateSensors) {
            heartRateSensors += AndroidRemoteQuantityInput(
                this,
                "${AndroidGateTypeId.HEART_RATE}$i",
                Frequency::class
            )
        }
        heartRateSensors
    }

    public override val lightSensors: List<AndroidRemoteQuantityInput<Illuminance>> = run {
        val lightSensors = ArrayList<AndroidRemoteQuantityInput<Illuminance>>()
        for (i in 0 until info.numLightSensors) {
            lightSensors += AndroidRemoteQuantityInput(
                this,
                "${AndroidGateTypeId.LIGHT}$i",
                Illuminance::class
            )
        }
        lightSensors
    }

    public override val pressureSensors: List<AndroidRemoteQuantityInput<Pressure>> = run {
        val pressureSensors = ArrayList<AndroidRemoteQuantityInput<Pressure>>()
        for (i in 0 until info.numPressureSensors) {
            pressureSensors += AndroidRemoteQuantityInput(
                this,
                "${AndroidGateTypeId.PRESSURE}$i",
                Pressure::class
            )
        }
        pressureSensors
    }

    public override val proximitySensors: List<AndroidRemoteQuantityInput<Length>> = run {
        val proximitySensors = ArrayList<AndroidRemoteQuantityInput<Length>>()
        for (i in 0 until info.numProximitySensors) {
            proximitySensors += AndroidRemoteQuantityInput(
                this,
                "${AndroidGateTypeId.PROXIMITY}$i",
                Length::class
            )
        }
        proximitySensors
    }
    public override val relativeHumiditySensors: List<AndroidRemoteQuantityInput<Dimensionless>> = run {
        val relativeHumiditySensors = ArrayList<AndroidRemoteQuantityInput<Dimensionless>>()
        for (i in 0 until info.numRelativeHumiditySensors) {
            relativeHumiditySensors += AndroidRemoteQuantityInput(
                this,
                "${AndroidGateTypeId.RELATIVE_HUMIDITY}$i",
                Dimensionless::class
            )
        }
        relativeHumiditySensors
    }

    public override val torchControllers: List<AndroidRemoteBinaryStateOutput> = run {
        val torchControllers = ArrayList<AndroidRemoteBinaryStateOutput>()
        for (i in 0 until info.numTorchControllers) {
            torchControllers += AndroidRemoteBinaryStateOutput(
                this,
                "${AndroidGateTypeId.TORCH}$i"
            )
        }
        torchControllers
    }

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)
        ambientTemperatureSensors.addSideRoutingTo(routing)
        heartRateSensors.addSideRoutingTo(routing)
        lightSensors.addSideRoutingTo(routing)
        pressureSensors.addSideRoutingTo(routing)
        proximitySensors.addSideRoutingTo(routing)
        relativeHumiditySensors.addSideRoutingTo(routing)
        torchControllers.addSideRoutingTo(routing)
    }

    public override fun toString(): String {
        return "RemoteAndroidDevice(uid=$uid, hostIp=$hostIp, job=${coroutineContext[Job]})"
    }
}

//TODO: Error handling
public suspend fun CoroutineScope.RemoteAndroidDeivce(hostIp: String): RemoteAndroidDevice {
    val info = Json.parse(AndroidDevice.Info.serializer(), FSRemoteDevice.getInfo(hostIp))


    return RemoteAndroidDevice(
        this,
        hostIp,
        info
    )
}