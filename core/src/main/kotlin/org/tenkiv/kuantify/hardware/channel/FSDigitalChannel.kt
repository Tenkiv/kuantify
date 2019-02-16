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

package org.tenkiv.kuantify.hardware.channel

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import javax.measure.quantity.*

internal fun CombinedRouteConfig.digitalChannelRouting(digitalChannel: DigitalChannel<*>, uid: String) {
    val channelRoute = listOf(RC.DAQC_GATE, uid)

    route(channelRoute + RC.AVG_FREQUENCY) to handler<ComparableQuantity<Frequency>>(isFullyBiDirectional = true) {
        serializeMessage {
            Json.stringify(ComparableQuantitySerializer, it)
        } withSerializer {
            receiveMessageOnEither {
                val setting = Json.parse(ComparableQuantitySerializer, it).asType<Frequency>().toDaqc()
                digitalChannel.avgFrequency.set(setting)
            }
        }

        setLocalUpdateChannel(digitalChannel.avgFrequency.updateBroadcaster.openSubscription()) withUpdateChannel {
            sendFromRemote()
            sendFromHost()
        }
    }

    route(channelRoute + RC.STOP_TRANSCEIVING) to handler<Ping>(isFullyBiDirectional = false) {
        receivePingOnEither {
            digitalChannel.stopTransceiving()
        }
    }
}

internal fun SideRouteConfig.digitalChannelIsTransceivingRemote(
    updatable: Updatable<Boolean>,
    uid: String,
    transceivingRC: String
) {
    route(RC.DAQC_GATE, uid, transceivingRC) to handler<Boolean>(isFullyBiDirectional = false) {
        receiveMessage(NullResolutionStrategy.PANIC) {
            val value = Json.parse(BooleanSerializer, it)
            updatable.set(value)
        }
    }
}

internal fun SideRouteConfig.digitalChannelIsTransceivingLocal(
    trackable: Trackable<Boolean>,
    uid: String,
    transceivingRC: String
) {
    route(RC.DAQC_GATE, uid, transceivingRC) to handler<Boolean>(isFullyBiDirectional = false) {
        serializeMessage {
            Json.stringify(BooleanSerializer, it)
        }

        setLocalUpdateChannel(trackable.updateBroadcaster.openSubscription()) withUpdateChannel {
            send()
        }
    }
}