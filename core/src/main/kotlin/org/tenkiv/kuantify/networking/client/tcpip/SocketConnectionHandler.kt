package org.tenkiv.kuantify.networking.client.tcpip

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.tls.tls
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.consumesAll
import kotlinx.coroutines.experimental.io.SuspendableReadSession
import kotlinx.io.core.IoBuffer
import org.tenkiv.kuantify.networking.ConnectionHandler
import org.tenkiv.kuantify.networking.ConnectionProtocol
import org.tenkiv.kuantify.networking.TransportProtocol
import org.tenkiv.kuantify.networking.UnsupportedProtocolException
import kotlin.coroutines.experimental.CoroutineContext

abstract class SocketConnectionHandler<T : Socket>(final override val connectionProtocol: ConnectionProtocol) :
    ConnectionHandler {

    override val coroutineContext: CoroutineContext = Job()

    init {
        if (!(connectionProtocol is TransportProtocol.Udp ||
            connectionProtocol is TransportProtocol.Tcp)
        )
            throw UnsupportedProtocolException()
    }

    @Volatile
    var socketStatus: SocketStatus = DisconnectedSocket

    override fun disconnect() = (socketStatus as? ConnectedSocket<*>)?.socket?.close() ?: Unit

    override fun connect() {
        launch {
            socketStatus = ConnectedSocket(buildSocket())
        }
    }

    protected abstract suspend fun buildSocket(dispatcher: CoroutineDispatcher = ioCoroutineDispatcher): T

}

open class TcpSocketHandler(connectionProtocol: ConnectionProtocol, val tlsEnabled: Boolean) :
    SocketConnectionHandler<Socket>(connectionProtocol) {

    val tcpConnectionProtocol: TransportProtocol.Tcp = connectionProtocol as TransportProtocol.Tcp


    override suspend fun buildSocket(dispatcher: CoroutineDispatcher): Socket {

        val socket = aSocket(ActorSelectorManager(dispatcher)).tcp().connect(tcpConnectionProtocol.socketAddress)

        return if (tlsEnabled)
            socket.tls()
        else
            socket
    }

    override fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean =
        connectionProtocol is TransportProtocol.Tcp

}

open class UdpSocketHandler(connectionProtocol: ConnectionProtocol) :
    SocketConnectionHandler<Socket>(connectionProtocol) {

    val udpConnectionProtocol: TransportProtocol.Udp = connectionProtocol as TransportProtocol.Udp

    override suspend fun buildSocket(dispatcher: CoroutineDispatcher): Socket {

        val socket = aSocket(ActorSelectorManager(dispatcher)).udp().connect(udpConnectionProtocol.socketAddress)

        return socket as Socket
    }

    override fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean =
        connectionProtocol is TransportProtocol.Udp

}

sealed class SocketStatus

data class ConnectedSocket<T : Socket>(val socket: T) : SocketStatus()

object DisconnectedSocket : SocketStatus()
