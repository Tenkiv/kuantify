package org.tenkiv.kuantify.networking

import io.kotlintest.specs.StringSpec
import io.ktor.network.sockets.openReadChannel
import kotlinx.coroutines.experimental.io.SuspendableReadSession
import kotlinx.coroutines.experimental.io.readRemaining
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.io.core.IoBuffer
import org.tenkiv.kuantify.networking.client.tcpip.ConnectedSocket
import org.tenkiv.kuantify.networking.client.tcpip.TcpSocketHandler
import java.net.InetSocketAddress

class ConnectionHandlerTest : StringSpec({
    "Test TCP Connection"{

        val socketAddress = InetSocketAddress("172.217.9.142", 80)

        val tcpConnection =
            TcpSocketHandler(TransportProtocol.Tcp(socketAddress), false)

        tcpConnection.connect()

        val status = tcpConnection.socketStatus

        if(status is ConnectedSocket<*>) launch(newSingleThreadContext("")) {
            val bytes = status.socket.openReadChannel().readRemaining()

            println("Size "+bytes.remaining)
        }else
            println("Is Invalid")
        //println("Socket Read "+String(it.array()))

        Thread.sleep(20000)
    }
})