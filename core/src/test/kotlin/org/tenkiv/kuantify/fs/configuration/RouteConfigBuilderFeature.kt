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
 *
 */

package org.tenkiv.kuantify.fs.configuration

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import mu.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.communication.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.networking.communication.*
import org.tenkiv.kuantify.networking.configuration.*

val logger = KotlinLogging.logger {}

object RouteConfigBuilderFeature : Spek({
    val localDevice = mockkClass(LocalDevice::class)
    every { localDevice.coroutineContext } returns GlobalScope.coroutineContext

    val localNetworkCommunicator = mockkClass(LocalNetworkCommunicator::class)
    every { localNetworkCommunicator.device } returns localDevice

    val remoteDevice = mockkClass(FSRemoteDevice::class)
    every { remoteDevice.coroutineContext } returns GlobalScope.coroutineContext

    val remoteNetworkCommunicator = mockkClass(FSRemoteWebsocketCommunicator::class)
    every { remoteNetworkCommunicator.device } returns remoteDevice
    every { remoteNetworkCommunicator.communicationMode } returns CommunicationMode.NON_EXCLUSIVE

    Feature("CombinedRouteConfig") {
        val configWithLocalDevice by memoized {
            CombinedRouteConfig(
                localNetworkCommunicator
            )
        }
        val configWithRemoteDevice by memoized {
            CombinedRouteConfig(
                remoteNetworkCommunicator
            )
        }

        val builder by memoized { CombinedRouteBindingBuilder<String>() }
        val build: CombinedRouteBindingBuilder<String>.() -> Unit = {
            setLocalUpdateChannel(Channel()) withUpdateChannel {
                sendFromHost()
                sendFromRemote()
            }

            serializeMessage {
                it
            } withSerializer {
                receiveMessageOnEither {

                }
            }
        }

        Scenario("creating route binding builder") {
            When("creating route binding builder that sends and receives messages on both sides") {
                builder.build()
            }

            Then("builder should have non-null message serializer") {
                assert(builder.serializeMessage != null)
            }

            Then("builder send from host should be true") {
                assert(builder.sendFromHost)
            }

            Then("builder send from remote should be true") {
                assert(builder.sendFromRemote)
            }

            Then("builder receive message on either should be non-null") {
                assert(builder.withSerializer?.receiveMessageOnEither != null)
            }
        }

        Scenario("adding route binding to local device config") {
            builder.build()

            When("adding binding to config") {
                configWithLocalDevice.baseRoute.route("a", "b", "c") {
                    bind("d", recursiveSynchronizer = false, build = build)
                }
            }

            val networkBindingPath = "a/b/c/d"

            Then("network config should contain binding for route") {
                assert(configWithLocalDevice.networkRouteBindingMap.contains(networkBindingPath))
            }

            Then("route binding should be of type standard") {
                assert(configWithLocalDevice.networkRouteBindingMap[networkBindingPath] is StandardRouteBinding)
            }

        }

        Scenario("adding route binding to remote device config") {
            builder.build()

            When("adding recursive synchronizer to config") {
                configWithRemoteDevice.baseRoute.route("a", "b", "c") {
                    bind("d", recursiveSynchronizer = true, build = build)
                }
            }

            val networkBindingPath = "a/b/c/d"

            Then("network config should contain binding for route") {
                assert(configWithRemoteDevice.networkRouteBindingMap.contains(networkBindingPath))
            }

            Then("route binding should be of type recursion preventing") {
                assert(
                    configWithRemoteDevice.networkRouteBindingMap[networkBindingPath]
                            is RecursionPreventingRouteBinding
                )
            }

        }

    }

    Feature("SideRouteConfig") {
        val configWithLocalDevice by memoized {
            SideRouteConfig(
                networkCommunicator = localNetworkCommunicator,
                serializedPing = FSDevice.serializedPing,
                formatPath = ::formatPathStandard
            )
        }
        val configWithRemoteDevice by memoized {
            SideRouteConfig(
                networkCommunicator = remoteNetworkCommunicator,
                serializedPing = FSDevice.serializedPing,
                formatPath = ::formatPathStandard
            )
        }

        val builder by memoized { SideRouteBindingBuilder<String, String>() }
        val build: SideRouteBindingBuilder<String, String>.() -> Unit = {

            serializeMessage {
                it
            }

            setLocalUpdateChannel(Channel()) withUpdateChannel {
                send()
            }

            receive {

            }
        }

        Scenario("creating route binding builder") {
            When("builing route binding builder that sends and receives messages") {
                builder.build()
            }

            Then("builder should have non-null message serializer") {
                assert(builder.serializeMessage != null)
            }

            Then("builder send should be true") {
                assert(builder.send)
            }

            Then("builder receive should be non-null") {
                assert(builder.receive != null)
            }
        }


        Scenario("adding route binding to local device config") {
            builder.build()

            When("adding binding to config") {
                configWithLocalDevice.baseRoute.route("a", "b", "c") {
                    bind("d", build = build)
                }
            }

            val networkBindingPath = "a/b/c/d"

            Then("network config should contain binding for route") {
                assert(configWithLocalDevice.networkRouteBindingMap.contains(networkBindingPath))
            }

            Then("route binding should be of type standard") {
                assert(configWithLocalDevice.networkRouteBindingMap[networkBindingPath] is StandardRouteBinding)
            }

        }

        Scenario("adding route binding to remote device config") {
            builder.build()

            When("adding recursive synchronizer to config") {
                configWithRemoteDevice.baseRoute.route("a", "b", "c") {
                    bind("d", recursiveSynchronizer = true, build = build)
                }
            }

            val networkBindingPath = "a/b/c/d"

            Then("network config should contain binding for route") {
                assert(configWithRemoteDevice.networkRouteBindingMap.contains(networkBindingPath))
            }

            Then("route binding should be of type recursion preventing") {
                assert(
                    configWithRemoteDevice.networkRouteBindingMap[networkBindingPath]
                            is RecursionPreventingRouteBinding
                )
            }

        }

    }
})