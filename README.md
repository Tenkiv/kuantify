# Kuantify &nbsp;[![License](http://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT) 

**Kuantify is usable in its current state but it's in a very early pre-alpha stage of development. Some things will not 
work and you should expect relatively frequent breaking changes.**

Kuantify is a data acquisition and control library written in Kotlin. Its purpose it to provide a common set of 
interfaces and abstractions for utilizing arbitrary collection and control endpoints in a safe, reasonable, and 
performant way. 
Kuantify is focused on enabling rapid prototyping of data acquisition and control programs and a smooth transition 
from prototypes to production.

Kuantify currently only works on the JVM and Android but it is a core design principle for it to ultimately be 
multiplatform and multiplatform support will be added as the api starts to stabilize [(the main thing we need to do is 
rewrite Physikal in pure Kotlin so it's usable on all Kotlin targets)](https://gitlab.com/tenkiv/software/physikal/issues/2). 

We also plan to create another repository where we centralize support for as many DAQC devices as possible but again, we
actually want to keep the number of supported devices to a minimum until we stabilize the api so we don't end up with a
maintenance nightmare. For now we only support the sensors and physical controls build into Android. Although Android
phone sensors are not really a big intended use case for this library, they provide a great accessible way to start
getting a feel for Kuantify and you can actually throw together some fairly interesting demos.

## Artifacts
| | Core | Android Core | Learning
 ------- | :-----: | :-----: | :-----:
 JVM | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.tenkiv.kuantify/kuantify-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.tenkiv.kuantify/kuantify-core) | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.tenkiv.kuantify/kuantify-android-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.tenkiv.kuantify/kuantify-android-core) | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.tenkiv.kuantify/kuantify-learning/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.tenkiv.kuantify/kuantify-learning)
 
 Local (for adapting OS support for device sensors and controllers to Kuantify):
 * Android Local - [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.tenkiv.kuantify/kuantify-android-local/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.tenkiv.kuantify/kuantify-android-local)

## Basics
First of all, in the name of type safety, Kuantify never passes around data as raw numbers. We use `DaqcValue` which is
either a `BinaryState` (high/low) or a `Quantity`. A `Quantity` is a type safe dimensioned number (a number with a unit),
`Quantity` support is done in a separate library called [Physikal](https://gitlab.com/tenkiv/software/physikal). You
should probably play with Physikal a bit before attempting to use Kuantify.

The three most important types to understand in Kuantify are `Input`, `Output`, and `Device`. Here's a super bare
bones usage example with an Android device.
```$xslt
val device = LocalAndroidDevice.get(this)
val lightSensor = device.lightSensors.first()

lightSensor.startSampling()
lightSensor.updateBroadcaster.consumeEach { measurement ->
    // Do something with every measurement
}

```

This example assumes your program is actually running in Android.

```
val device = LocalAndroidDevice.get(this)
```

Gets the device it's running on as a Kuantify device. Since different
Android devices have different sensors and controllers available each type of sensor or controller is given as a list
that may be empty if the device doesn't support a given type.

```
lightSensor.startSampling()
```

Starts sampling the light sensor; you won't receive any updates from it until you start it.

```
lightSensor.updateBroadcaster.consumeEach
```

Each `Input` has a [BroadcastChannel](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/index.html)
called `updateBroadcaster` that broadcasts updates from that `Input`. Here we're just consuming all updates.

Using an `Output` is very similar.
```$xslt
val device = LocalAndroidDevice.get(this)
val torchController = device.torchControllers.first()

torchController.setOutput(BinaryState.High)
```


In the above examples we're using Kuantify on the device with the sensors and controllers. But Kuantify is primarily
intended to be used on a device that is connected to other devices which actually have the sensors and controllers.
So let's change our example to now run on a computer that is connected to an Android device. The Android device is now a
`RemoteDevice`. Note: to do this we need to run a host utility on the Android Device we're connecting to. This utility
is trivial to make but you can just install android-simple-host to get started with remote connections. More information
in the "Supporting Devices" section.

```$xslt

val device = RemoteAndroidDeivce("192.168.1.5")
device.connect()
val lightSensor = device.lightSensors.first()

lightSensor.startSampling()

lightSensor.updateBroadcaster.consumeEach { measurement ->
    // Do something with every measurement
}

```

The only difference here is we now first connect to the device before using it. We're running everything in some
suspending block now because both creating a `RemoteAndroidDevice` and connecting to one is a suspending operation.

In `val device = RemoteAndroidDeivce("192.168.1.5:8080")` the parameter passed to `RemoteAndroidDevice()` is the IP
address of the device, in this example it's on the local network but it doesn't have to be. We have a `Locator` class 
for automating the process of finding and connecting to remote devices but it's not yet implemented for Android Devices.

## Supporting Devices
There are two categories of device Kuantify can support.
1. Devices that are intended to be controlled remotely and run some firmware facilitating remote connections. Examples
include National Instruments / Agilent data acquisition systems and Arduino boards.
1. Devices which are intended to have software installed directly on them. Examples include standard Android devices and
Raspberry Pi.

Both categories utilize Kuantifies built in message routing system for communication, although in the case of full stack
devices most of the routing should be handled automatically.

### First Category - Remote Only Devices
Adding support for a remote only device is basically a matter of binding the functionality of the device to the Kuantify
interfaces. So you'll create a class that extends `Device` and potentially some of the other Device interfaces depending
on the functionality of the device you're adding support for. In most cases this process is a
matter of establishing some connection, serializing / deserialzing commands and incoming data and sending / receiving
these things to and from the device. Most of this process will be handled with a custom `NetworkCommunicator`.
Kuantify already depends on [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
and parts of [ktor](https://ktor.io/) so you should consider using them for these tasks. [Ktor supports raw tcp and udp
sockets](https://ktor.io/clients/raw-sockets.html) which are often necessary for supporting a device. Both of these
libraries are pure kotlin and multiplatform so you only need to make one set of common code to support the device on all
platforms if you use them.

### Second Category - Full Stack Devices
To support the second category we use Kuantify's "full stack" abstractions because we will be facilitating both the
hosting / serving on device that has the sensors and controls and the remote connection and control 
on any device we may want to interact with the host device from. A full stack device can be used locally only or
remotely only but support should be added for both, hence the name. There are three steps for adding support for a full
stack device.
1. Extend interface `FSDevice` for the device to define what `Input`s, `Output`s and potentially other functionality is 
has available.
1. Extend `LocalDevice` to create Kuantify bindings for the devices functionality.
1. Extend `FSRemoteDevice` so the device can be connected to remotely. This step should be very simple as in most cases
Kuantify will automatically handle all the communication between the local and remote device so long as the local device
is set up correctly.

This structure helps programs be more maintainable because the majority of the code can be written in a way where it
doesn't care if it's running on the device itself or it is remotely connected to the device.
