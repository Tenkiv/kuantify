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

package org.tenkiv.kuantify.networking.configuration

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import mu.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.networking.device.*

val logger = KotlinLogging.logger {}

object RouteConfigBuilderFeature : Spek({
    Feature("CombinedRouteConfig") {
        val configWithLocalDevice by memoized {
            val device = mockkClass(LocalDevice::class)
            every { device.coroutineContext } returns GlobalScope.coroutineContext

            CombinedRouteConfig(device)
        }

        val configWithRemoteDevice by memoized {

        }

        Scenario("adding route bindings") {

            When("adding route binding that sends and receives messages on both sides") {
                configWithLocalDevice.baseRoute.route("a", "b", "c") {
                    bind<String>("d", isFullyBiDirectional = true) {
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
                }
            }

            val networkBindingPath = listOf("a", "b", "c", "d")

            Then("network config should contain binding for route") {
                assert(configWithLocalDevice.networkRouteBindingMap.contains(networkBindingPath))
            }

            Then("route binding should be of type host") {
                assert(configWithLocalDevice.networkRouteBindingMap[networkBindingPath] is NetworkRouteBinding.Host)
                assert(configWithLocalDevice.networkRouteBindingMap[networkBindingPath] !is NetworkRouteBinding.Host)
            }

            Then("route binding should have non-null message serializer") {

            }

            Then("route binding send from host should be true") {

            }

            Then("route binding send from remote should be true") {

            }

            Then("route binding receive message on either should be non-null") {

            }

            Then("route config should contain route binding") {

            }

        }

    }

    Feature("SideRouteConfig") {

    }
})