package org.tenkiv.kuantify.networking.client.tcpip

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.tls.tls
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.io.core.IoBuffer
import org.tenkiv.kuantify.networking.*
import java.nio.ByteBuffer
import kotlin.coroutines.experimental.CoroutineContext

abstract class SocketConnectionHandler<T : ASocket, I, O>(final override val connectionProtocol: ConnectionProtocol) :
    ConnectionHandler<I, O> {

    override val coroutineContext: CoroutineContext = Job()

    override var handlerStatus: HandlerConnectionStatus = UnconnectedHandler

    init {
        if (!(connectionProtocol is TransportProtocol.Udp ||
                    connectionProtocol is TransportProtocol.Tcp)
        )
            throw UnsupportedProtocolException()
    }

    override suspend fun disconnect(): HandlerConnectionStatus {
        coroutineContext.cancel()
        handlerStatus = UnconnectedHandler
        return handlerStatus
    }

    protected abstract suspend fun buildSocket(dispatcher: CoroutineDispatcher = ioCoroutineDispatcher): T

}

open class TcpSocketHandler<I, O>(
    connectionProtocol: ConnectionProtocol,
    val tlsEnabled: Boolean,
    val sendHandler: (I) -> ByteBuffer,
    val receiveSize: Int,
    val receiveHandler: (IoBuffer?) -> O
) :
    SocketConnectionHandler<Socket, I, O>(connectionProtocol) {
    val tcpConnectionProtocol: TransportProtocol.Tcp = connectionProtocol as TransportProtocol.Tcp


    override suspend fun connect(): HandlerConnectionStatus {

        val socket = buildSocket()

        val inputStream = socket.openWriteChannel(true)

        val outputStream = socket.openReadChannel()

        val sender = actor<I> {
            consumeEach {
                inputStream.writeAvailable(sendHandler(it))
            }
        }

        val receiver = produce<O> {
            outputStream.readSuspendableSession {
                this.await(receiveSize)
                send(receiveHandler(this.request(receiveSize)))
            }
        }

        val status = ConnectedHandler(receiver, sender)

        handlerStatus = status

        return status

    }


    override suspend fun buildSocket(dispatcher: CoroutineDispatcher): Socket {

        val socket =
            aSocket(ActorSelectorManager(dispatcher)).tcp().connect(tcpConnectionProtocol.socketAddress)

        return if (tlsEnabled)
            socket.tls()
        else
            socket
    }

    override fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean =
        connectionProtocol is TransportProtocol.Tcp

}

open class UdpSocketHandler(connectionProtocol: ConnectionProtocol) :
    SocketConnectionHandler<ConnectedDatagramSocket, Datagram, Datagram>(connectionProtocol) {

    val udpConnectionProtocol: TransportProtocol.Udp = connectionProtocol as TransportProtocol.Udp

    override suspend fun connect(): HandlerConnectionStatus {

        val socket = buildSocket()

        val status = ConnectedHandler(socket.incoming, socket.outgoing)

        handlerStatus = status

        return status

    }

    override suspend fun buildSocket(dispatcher: CoroutineDispatcher): ConnectedDatagramSocket {
        return aSocket(ActorSelectorManager(dispatcher)).udp().connect(udpConnectionProtocol.socketAddress)
    }

    override fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean =
        connectionProtocol is TransportProtocol.Udp

}