/*
package org.tenkiv.kuantify.networking.client.tcpip

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.tls.tls
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.io.core.IoBuffer
import org.tenkiv.kuantify.networking.*
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

abstract class SocketConnectionHandler<I,O,T : ASocket>(override val connectionProtocol: ConnectionProtocol) :
    ConnectionHandler<I,O> {

    override val coroutineContext: CoroutineContext = Job()

    override var handlerStatus: HandlerConnectionStatus.ConnectedHandler<I, O> = HandlerConnectionStatus.UnconnectedHandler

    init {
        if (!(connectionProtocol is TransportProtocol.Udp ||
                    connectionProtocol is TransportProtocol.Tcp)
        )
            throw UnsupportedProtocolException()
    }

    override suspend fun disconnect() {
        coroutineContext.cancel()
        handlerStatus = null
    }

    protected abstract suspend fun buildSocket(dispatcher: CoroutineDispatcher = ioCoroutineDispatcher): T

}

*/
/*open class RawTcpSocketHandler(connectionProtocol: ConnectionProtocol,
                            val tlsEnabled: Boolean) :
    SocketConnectionHandler<ByteWriteChannel,ByteReadChannel,Socket>(connectionProtocol) {

    val tcpConnectionProtocol: TransportProtocol.Tcp = connectionProtocol as TransportProtocol.Tcp

    var inputStream: ByteWriteChannel? = null

    var outputStream: ByteReadChannel? = null

    override suspend fun buildSocket(dispatcher: CoroutineDispatcher): Socket {
        val socket =
            aSocket(ActorSelectorManager(dispatcher)).tcp().connect(tcpConnectionProtocol.socketAddress)

        return if (tlsEnabled)
            socket.tls(this.coroutineContext)
        else
            socket
    }

    override suspend fun connect(): HandlerConnectionStatus.ConnectedHandler<ByteWriteChannel, ByteReadChannel> {
        val socket = buildSocket()

        val inSt = socket.openWriteChannel(true)
        inputStream = inSt
        val outSt = socket.openReadChannel()
        outputStream = outSt

        return HandlerConnectionStatus.ConnectedHandler(inSt, outSt)
    }

    override fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean =
        connectionProtocol is TransportProtocol.Tcp
}*//*


open class TcpSocketHandler<I, O>(
    connectionProtocol: ConnectionProtocol,
    val tlsEnabled: Boolean,
    val sendHandler: (O) -> ByteBuffer,
    val receiveSize: Int,
    val receiveHandler: (IoBuffer?) -> I
) :
    SocketConnectionHandler<Socket, I, O>(connectionProtocol) {
    val tcpConnectionProtocol: TransportProtocol.Tcp = connectionProtocol as TransportProtocol.Tcp

    override suspend fun connect(): ConnectedHandler<I, O> {

        val socket = buildSocket()

        val inputStream = socket.openWriteChannel(true)

        val outputStream = socket.openReadChannel()

        val sender = actor<O> {
            consumeEach {
                inputStream.writeFully(sendHandler(it))
                inputStream.flush()
            }
        }

        val receiver = produce<I> {
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
            socket.tls(this.coroutineContext)
        else
            socket
    }

    override fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean =
        connectionProtocol is TransportProtocol.Tcp

}

open class UdpSocketHandler(override val connectionProtocol: TransportProtocol.Udp) :
    SocketConnectionHandler<ConnectedDatagramSocket>(connectionProtocol) {

    override suspend fun connect(): ConnectedHandler<ReceiveChannel<Datagram>,SendChannel<Datagram>> {

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

}*/
