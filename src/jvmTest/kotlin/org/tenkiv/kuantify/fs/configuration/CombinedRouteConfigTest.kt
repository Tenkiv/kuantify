/*
 * Copyright 2020 Tenkiv, Inc.
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

package org.tenkiv.kuantify.fs.configuration

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.communication.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.networking.communication.*
import org.tenkiv.kuantify.networking.configuration.*
import kotlin.test.*

private fun localDevice() = mockkClass(LocalDevice::class).apply {
    every { coroutineContext } returns GlobalScope.coroutineContext
}

private fun localNetworkCommunicator(localDevice: LocalDevice) =
    mockkClass(LocalNetworkCommunicator::class).apply {
        every { device } returns localDevice()
    }

private fun remoteDevice() = mockkClass(FSRemoteDevice::class).apply {
    every { coroutineContext } returns GlobalScope.coroutineContext
}

private fun remoteNetworkCommunicator(remoteDevice: FSRemoteDevice) =
    mockkClass(FSRemoteWebsocketCommunicator::class).apply {
        every { device } returns remoteDevice
        every { communicationMode } returns CommunicationMode.NON_EXCLUSIVE
    }

private fun combinedRouteBindingBuilder() = CombinedRouteBindingBuilder<String>()

private val buildCombinedRouteBinding: CombinedRouteBindingBuilder<String>.() -> Unit = {
    setLocalUpdateChannel(Channel()) withUpdateChannel {
        sendFromHost()
        sendFromRemote()
    }
    serializeMessage { it } withSerializer { receiveMessageOnEither { } }
}

private fun sideRouteBindingBuilder() = SideRouteBindingBuilder<String, String>()

private val buildSideRouteBinding: SideRouteBindingBuilder<String, String>.() -> Unit = {
    serializeMessage {
        it
    }

    setLocalUpdateChannel(Channel()) withUpdateChannel {
        send()
    }

    receive {

    }

}

class RouteBindingBuilderTest {
    private val builder = combinedRouteBindingBuilder()

    @BeforeTest
    fun `create route binding builder that sends and receives messages on both sides`() {
        builder.buildCombinedRouteBinding()
    }

    @Test
    fun `builder should have non-null message serializer`() {
        assert(builder.serializeMessage != null)
    }

    @Test
    fun `builder send from host should be true`() {
        assert(builder.sendFromHost)
    }

    @Test
    fun `builder send from remote should be true`() {
        assert(builder.sendFromRemote)
    }

    @Test
    fun `builder receive message on either should be non-null`() {
        assert(builder.withSerializer?.receiveMessageOnEither != null)
    }
}

class CombinedRouteLocalDeviceConfigTest {
    private val builder = combinedRouteBindingBuilder()
    private val localDevice = localDevice()
    private val localNetworkCommunicator = localNetworkCommunicator(localDevice)
    private val configWithLocalDevice = CombinedRouteConfig(localNetworkCommunicator)
    private val networkBindingPath = "a/b/c/d"

    @BeforeTest
    fun `add binding to config`() {
        builder.buildCombinedRouteBinding()
        configWithLocalDevice.baseRoute.route("a", "b", "c") {
            bind("d", recursiveSynchronizer = false, build = buildCombinedRouteBinding)
        }
    }

    @Test
    fun `network config should contain binding for route`() {
        assert(configWithLocalDevice.networkRouteBindingMap.contains(networkBindingPath))
    }

    @Test
    fun `route binding should be of type standard`() {
        assert(configWithLocalDevice.networkRouteBindingMap[networkBindingPath] is StandardRouteBinding)
    }
}

class CombinedRouteRemoteDeviceConfigTest {
    private val builder = combinedRouteBindingBuilder()
    private val remoteDevice = remoteDevice()
    private val remoteNetworkRouting = remoteNetworkCommunicator(remoteDevice)
    private val configWithRemoteDevice = CombinedRouteConfig(remoteNetworkRouting)
    private val networkBindingPath = "a/b/c/d"

    @BeforeTest
    fun `add recursive synchronizer to config`() {
        builder.buildCombinedRouteBinding()
        configWithRemoteDevice.baseRoute.route("a", "b", "c") {
            bind("d", recursiveSynchronizer = true, build = buildCombinedRouteBinding)
        }
    }

    @Test
    fun `network config should contain binding for route`() {
        assert(configWithRemoteDevice.networkRouteBindingMap.contains(networkBindingPath))
    }

    @Test
    fun `route binding should be of type recursion preventing`() {
        assert(configWithRemoteDevice.networkRouteBindingMap[networkBindingPath] is RecursionPreventingRouteBinding)
    }
}

class SideRouteBindingBuilderTest {
    private val builder = sideRouteBindingBuilder()

    @BeforeTest
    fun `build route binding builder that sends and receives messages`() {
        builder.buildSideRouteBinding()
    }

    @Test
    fun `builder should have non-null message serializer`() {
        assert(builder.serializeMessage != null)
    }

    @Test
    fun `builder send should be true`() {
        assert(builder.send)
    }

    @Test
    fun `builder receive should be non-null`() {
        assert(builder.receive != null)
    }
}

class SideRouteBindingLocalDeviceConfigTest {
    private val localDevice = localDevice()
    private val localNetworkCommunicator = localNetworkCommunicator(localDevice)
    private val configWithLocalDevice = SideRouteConfig(
        networkCommunicator = localNetworkCommunicator,
        serializedPing = FSDevice.serializedPing,
        formatPath = ::formatPathStandard
    )
    private val builder = sideRouteBindingBuilder()
    private val networkBindingPath = "a/b/c/d"

    @BeforeTest
    fun `add binding to config`() {
        builder.buildSideRouteBinding()
        configWithLocalDevice.baseRoute.route("a", "b", "c") {
            bind("d", build = buildSideRouteBinding)
        }
    }

    @Test
    fun `network config should contain binding for route`() {
        assert(configWithLocalDevice.networkRouteBindingMap.contains(networkBindingPath))
    }

    @Test
    fun `route binding should be of type standard`() {
        assert(configWithLocalDevice.networkRouteBindingMap[networkBindingPath] is StandardRouteBinding)
    }
}

class SideRouteBindingRemoteDeviceConfigTest {
    private val remoteDevice = remoteDevice()
    private val remoteNetworkCommunicator = remoteNetworkCommunicator(remoteDevice)
    private val configWithRemoteDevice = SideRouteConfig(
        networkCommunicator = remoteNetworkCommunicator,
        serializedPing = FSDevice.serializedPing,
        formatPath = ::formatPathStandard
    )

    private val builder = sideRouteBindingBuilder()
    private val networkBindingPath = "a/b/c/d"

    @BeforeTest
    fun `add recursive synchronizer to config`() {
        builder.buildSideRouteBinding()
        configWithRemoteDevice.baseRoute.route("a", "b", "c") {
            bind("d", recursiveSynchronizer = true, build = buildSideRouteBinding)
        }
    }

    @Test
    fun `network config should contain binding for route`() {
        assert(configWithRemoteDevice.networkRouteBindingMap.contains(networkBindingPath))
    }

    @Test
    fun `route binding should be of type recursion preventing`() {
        assert(configWithRemoteDevice.networkRouteBindingMap[networkBindingPath] is RecursionPreventingRouteBinding)
    }
}


