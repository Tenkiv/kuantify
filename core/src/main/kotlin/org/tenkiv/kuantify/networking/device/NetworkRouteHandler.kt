package org.tenkiv.kuantify.networking.device

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

typealias UpdateReceiver<R> = (R, update: String?) -> Unit
typealias MessageSerializer<T> = (update: T) -> String

internal sealed class NetworkRouteHandler<R : Any, T>(protected val device: KuantifyDevice) : CoroutineScope {

    @Volatile
    protected var job = Job(coroutineContext[Job])

    final override val coroutineContext: CoroutineContext
        get() = device.coroutineContext + job

    open fun start(job: Job) {
        this.job = job
    }

    internal class Host<R : Any, T> internal constructor(
        device: LocalDevice,
        private val route: Route,
        private val localReceiver: R,
        private val localUpdateChannel: ReceiveChannel<T>,
        private val networkUpdateChannel: ReceiveChannel<String?>,
        private val serializeMessage: MessageSerializer<T>?,
        private val sendUpdatesFromHost: Boolean,
        private val receiveUpdateOnHost: UpdateReceiver<R>?
    ) : NetworkRouteHandler<R, T>(device) {

        override fun start(job: Job) {
            super.start(job)
            // Send
            if (sendUpdatesFromHost) {
                launch {
                    localUpdateChannel.consumeEach {
                        val payload = serializeMessage?.invoke(it)
                        device.sendMessage(route, payload)
                    }
                }
            }

            // Receive
            if (receiveUpdateOnHost != null) {
                launch {
                    networkUpdateChannel.consumeEach {
                        receiveUpdateOnHost.invoke(localReceiver, it)
                    }
                }
            }
        }

    }

    internal class Remote<R : Any, T> internal constructor(
        device: RemoteKuantifyDevice,
        private val route: Route,
        private val localReceiver: R,
        private val localUpdateChannel: ReceiveChannel<T>,
        private val networkUpdateChannel: ReceiveChannel<String?>,
        private val serializeMessage: MessageSerializer<T>?,
        private val sendUpdatesFromRemote: Boolean,
        private val sendUpdatesFromHost: Boolean,
        private val receiveUpdateOnRemote: UpdateReceiver<R>?
    ) : NetworkRouteHandler<R, T>(device) {

        private val fullyBiDirectional get() = sendUpdatesFromHost && sendUpdatesFromRemote

        private val ignoreNextUpdate = AtomicBoolean(false)

        override fun start(job: Job) {
            super.start(job)
            // Send
            if (sendUpdatesFromRemote) {
                if (fullyBiDirectional) {
                    launch {
                        localUpdateChannel.consumeEach {
                            if (!ignoreNextUpdate.get()) {
                                val payload = serializeMessage?.invoke(it)
                                device.sendMessage(route, payload)
                            } else {
                                ignoreNextUpdate.set(false)
                            }
                        }
                    }
                } else {
                    launch {
                        localUpdateChannel.consumeEach {
                            val payload = serializeMessage?.invoke(it)
                            device.sendMessage(route, payload)
                        }
                    }
                }
            }

            // Receive
            if (receiveUpdateOnRemote != null) {
                if (fullyBiDirectional) {
                    launch {
                        networkUpdateChannel.consumeEach {
                            ignoreNextUpdate.set(true)
                            receiveUpdateOnRemote.invoke(localReceiver, it)
                        }
                    }
                } else {
                    launch {
                        networkUpdateChannel.consumeEach {
                            receiveUpdateOnRemote.invoke(localReceiver, it)
                        }
                    }
                }
            }
        }

    }

}
