package org.tenkiv.kuantify.networking.client.tcpip

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.tls.tls
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.io.core.IoBuffer
import org.tenkiv.kuantify.networking.*
import java.nio.ByteBuffer
import kotlin.coroutines.experimental.CoroutineContext

abstract class SocketConnectionHandler<T : ASocket, I, O>(override val connectionProtocol: ConnectionProtocol) :
    ConnectionHandler {

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

    override suspend fun connect(): ConnectedHandler<I, O> {

        val socket = buildSocket()

        val inputStream = socket.openWriteChannel(true)

        val outputStream = socket.openReadChannel()

        val sender = actor<I> {
            consumeEach {
                inputStream.writeFully(sendHandler(it))
                inputStream.flush()
            }
        }

        val receiver = produce<O> {
            outputStream.readSuspendableSession {
                while (isActive) {
                    await(receiveSize)
                    send(receiveHandler(request(receiveSize)))
                }
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

open class UdpSocketHandler(override val connectionProtocol: TransportProtocol.Udp) :
    SocketConnectionHandler<ConnectedDatagramSocket, Datagram, Datagram>(connectionProtocol) {

    override suspend fun connect(): HandlerConnectionStatus {

        val socket = buildSocket()

        val status = ConnectedHandler(socket.incoming, socket.outgoing)

        handlerStatus = status

        return status

    }

    override suspend fun buildSocket(dispatcher: CoroutineDispatcher): ConnectedDatagramSocket {
        return aSocket(ActorSelectorManager(dispatcher)).udp().connect(connectionProtocol.socketAddress)
    }

    override fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean =
        connectionProtocol is TransportProtocol.Udp

}