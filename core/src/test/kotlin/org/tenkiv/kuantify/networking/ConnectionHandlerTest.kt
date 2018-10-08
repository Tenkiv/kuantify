package org.tenkiv.kuantify.networking

import io.kotlintest.specs.StringSpec
import io.ktor.network.sockets.openReadChannel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.SuspendableReadSession
import kotlinx.coroutines.experimental.io.readRemaining
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.io.core.IoBuffer
import org.tenkiv.kuantify.networking.client.tcpip.TcpSocketHandler
import java.net.InetSocketAddress

class ConnectionHandlerTest : StringSpec({
    "Test TCP Connection"{

        val writeChannel = ByteReadChannel("Here is some text\n Here is more text\n Here is even more ext")

        /*launch {
            writeChannel.readSuspendableSession {
                println("Something?")
            }
        }

        delay(20000)*/

        /*val socketAddress = InetSocketAddress("172.217.9.142", 80)

        val tcpConnection =
            TcpSocketHandler(TransportProtocol.Tcp(socketAddress), false)

        tcpConnection.connect()*/

        //println("Socket Read "+String(it.array()))

        Thread.sleep(20000)
    }
})