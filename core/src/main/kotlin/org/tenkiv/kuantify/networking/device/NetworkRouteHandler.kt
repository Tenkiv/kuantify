package org.tenkiv.kuantify.networking.device

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

typealias UpdateReceiver = (update: String?) -> Unit
typealias MessageSerializer<T> = (update: T) -> String

internal sealed class NetworkRouteHandler<T>(protected val device: BaseKuantifyDevice) : CoroutineScope {

    @Volatile
    protected var job = Job(coroutineContext[Job])

    final override val coroutineContext: CoroutineContext
        get() = device.coroutineContext + job

    open fun start(job: Job) {
        this.job = job
    }

    internal class Host<T> internal constructor(
        device: BaseKuantifyDevice,
        private val route: Route,
        private val localUpdateChannel: ReceiveChannel<T>,
        private val networkUpdateChannel: ReceiveChannel<String?>,
        private val serializeMessage: MessageSerializer<T>?,
        private val sendUpdatesFromHost: Boolean,
        private val receiveUpdateOnHost: UpdateReceiver?
    ) : NetworkRouteHandler<T>(device) {

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
                        receiveUpdateOnHost.invoke(it)
                    }
                }
            }
        }
    }

    internal class Remote<T> internal constructor(
        device: BaseKuantifyDevice,
        private val route: Route,
        private val localUpdateChannel: ReceiveChannel<T>,
        private val networkUpdateChannel: ReceiveChannel<String?>,
        private val serializeMessage: MessageSerializer<T>?,
        private val sendUpdatesFromRemote: Boolean,
        private val receiveUpdateOnRemote: UpdateReceiver?,
        private val isFullyBiDirectional: Boolean
    ) : NetworkRouteHandler<T>(device) {

        private val ignoreNextUpdate = AtomicBoolean(false)

        override fun start(job: Job) {
            super.start(job)
            // Send
            if (sendUpdatesFromRemote) {
                if (isFullyBiDirectional) {
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
                if (isFullyBiDirectional) {
                    launch {
                        networkUpdateChannel.consumeEach {
                            ignoreNextUpdate.set(true)
                            receiveUpdateOnRemote.invoke(it)
                        }
                    }
                } else {
                    launch {
                        networkUpdateChannel.consumeEach {
                            receiveUpdateOnRemote.invoke(it)
                        }
                    }
                }
            }
        }

    }

}
