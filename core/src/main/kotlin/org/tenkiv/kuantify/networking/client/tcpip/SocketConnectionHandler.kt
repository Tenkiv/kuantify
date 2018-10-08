package org.tenkiv.kuantify.networking.client.tcpip

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.tls.tls
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.launch
import org.tenkiv.kuantify.networking.ConnectionHandler
import org.tenkiv.kuantify.networking.ConnectionProtocol
import org.tenkiv.kuantify.networking.TransportProtocol
import org.tenkiv.kuantify.networking.UnsupportedProtocolException
import kotlin.coroutines.experimental.CoroutineContext

abstract class SocketConnectionHandler<T : ASocket, I, O>(final override val connectionProtocol: ConnectionProtocol) :
    ConnectionHandler<I, O> {

    override val coroutineContext: CoroutineContext = Job()

    init {
        if (!(connectionProtocol is TransportProtocol.Udp ||
                    connectionProtocol is TransportProtocol.Tcp)
        )
            throw UnsupportedProtocolException()
    }

    override fun disconnect() {
        coroutineContext.cancel()
    }

    protected abstract suspend fun buildSocket(dispatcher: CoroutineDispatcher = ioCoroutineDispatcher): T

}

open class TcpSocketHandler<T>(
    connectionProtocol: ConnectionProtocol,
    val tlsEnabled: Boolean
) :
    SocketConnectionHandler<Socket, ByteReadChannel, ByteWriteChannel>(connectionProtocol) {
    val tcpConnectionProtocol: TransportProtocol.Tcp = connectionProtocol as TransportProtocol.Tcp

    override lateinit var inputStream: ByteReadChannel

    override lateinit var outputStream: ByteWriteChannel

    override fun connect() {
        launch {
            val socket = buildSocket()
            inputStream = socket.openReadChannel()
            outputStream = socket.openWriteChannel(true)

        }
    }


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

class Utf8TcpSocket(
    connectionProtocol: ConnectionProtocol,
    tlsEnabled: Boolean
) : TcpSocketHandler<String>(connectionProtocol, tlsEnabled)

open class UdpSocketHandler<T>(
    connectionProtocol: ConnectionProtocol
) :
    SocketConnectionHandler<ConnectedDatagramSocket, ReceiveChannel<Datagram>, SendChannel<Datagram>>(connectionProtocol) {

    val udpConnectionProtocol: TransportProtocol.Udp = connectionProtocol as TransportProtocol.Udp

    override lateinit var inputStream: ReceiveChannel<Datagram>

    override lateinit var outputStream: SendChannel<Datagram>

    override fun connect() {
        launch {
            val socket = buildSocket()
            inputStream = socket.incoming
            outputStream = socket.outgoing
        }
    }

    override suspend fun buildSocket(dispatcher: CoroutineDispatcher): ConnectedDatagramSocket {
        return aSocket(ActorSelectorManager(dispatcher)).udp().connect(udpConnectionProtocol.socketAddress)
    }

    override fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean =
        connectionProtocol is TransportProtocol.Udp

}