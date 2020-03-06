/*
 * Copyright 2020 Tenkiv, Inc.
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

package org.tenkiv.kuantify.fs.gate

import kotlinx.serialization.builtins.*
import kotlinx.serialization.internal.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.lib.physikal.*
import org.tenkiv.kuantify.networking.configuration.*
import physikal.*

internal fun CombinedNetworkRouting.digitalGateRouting(digitalChannel: DigitalGate) {
    bind<Quantity<Frequency>>(RC.AVG_FREQUENCY, recursiveSynchronizer = true) {
        serializeMessage {
            Serialization.json.stringify(Quantity.serializer(), it)
        } withSerializer {
            receiveMessageOnEither {
                val setting = Serialization.json.parse(Quantity.serializer<Frequency>(), it)
                digitalChannel.avgFrequency.set(setting)
            }
        }

        setLocalUpdateChannel(digitalChannel.avgFrequency.updateBroadcaster.openSubscription()) withUpdateChannel {
            sendFromRemote()
            sendFromHost()
        }
    }

    bind<Ping>(RC.STOP_TRANSCEIVING, recursiveSynchronizer = false) {
        receivePingOnEither {
            digitalChannel.stopTransceiving()
        }
    }
}

internal fun SideNetworkRouting<String>.digitalGateIsTransceivingRemote(
    updatable: Updatable<Boolean>,
    transceivingRC: String
) {
    bind<Boolean>(transceivingRC) {
        receive {
            val value = Serialization.json.parse(Boolean.serializer(), it)
            updatable.set(value)
        }
    }
}

internal fun SideNetworkRouting<String>.digitalGateIsTransceivingLocal(
    trackable: Trackable<Boolean>,
    transceivingRC: String
) {
    bind<Boolean>(transceivingRC) {
        serializeMessage {
            Serialization.json.stringify(Boolean.serializer(), it)
        }

        setLocalUpdateChannel(trackable.updateBroadcaster.openSubscription()) withUpdateChannel {
            send()
        }
    }
}