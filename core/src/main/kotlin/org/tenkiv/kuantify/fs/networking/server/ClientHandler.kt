/*
 * Copyright 2019 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.kuantify.fs.networking.server

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.sync.*
import mu.*
import org.tenkiv.kuantify.lib.*

private val logger = KotlinLogging.logger {}

internal object ClientHandler {

    private val mutexClients: MutexValue<MutableMap<String, HostedClient>> = MutexValue(HashMap(), Mutex())

    suspend fun connectionOpened(clientId: String, session: WebSocketSession) {
        mutexClients.withLock { clients ->
            if (clients.containsKey(clientId)) {
                clients[clientId]?.addSession(session)
            } else {
                clients[clientId] = HostedClient(clientId).apply {
                    addSession(session)
                }
            }
        }
    }

    suspend fun connectionClosed(clientId: String, session: WebSocketSession) {
        mutexClients.withLock { clients ->
            clients[clientId]?.removeSession(session, clients)
        }
    }

    suspend fun sendToAll(message: String) {
        mutexClients.withLock { clients ->
            clients.values.forEach {
                it.sendMessage(message)
            }
            logger.trace { "Sent message - $message - from local device" }
        }
    }

    suspend fun closeAllSessions() {
        mutexClients.withLock { clients ->
            clients.values.forEach { it.closeAllSessions() }
        }
    }

    private class HostedClient(val id: String) {

        private val websocketSessions: MutableList<WebSocketSession> = ArrayList()

        fun addSession(session: WebSocketSession) {
            websocketSessions += session
        }

        fun removeSession(session: WebSocketSession, clients: MutableMap<String, HostedClient>) {
            websocketSessions -= session
            if (websocketSessions.isEmpty()) clients -= id
        }

        suspend fun sendMessage(serializedMsg: String) {
            websocketSessions.forEach { it.send(Frame.Text(serializedMsg)) }
        }

        suspend fun closeAllSessions() {
            websocketSessions.forEach { it.close() }
        }

    }

}