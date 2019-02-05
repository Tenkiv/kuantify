package org.tenkiv.kuantify.hardware.definitions.channel

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