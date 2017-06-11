package com.tenkiv.daqc.monitoring

/**
 * Created by tenkiv on 5/16/17.
 */
class PIDController(val Kp: Double, val Ki: Double, val Kd: Double) {
    var error: Double = 0.0
    var previousError: Double = 0.0
    var integral: Double = 0.0
    var setPoint: Double = 0.0
    var lastValue: Double = 0.0

    var timeInterval = 0L

    init {
        while (true) {
            error = setPoint - lastValue
            integral += error * timeInterval
            val derivative = (error - previousError)
            val output = Kp * error + Ki * integral + Kd * derivative
            previousError = error
            Thread.sleep(timeInterval)
        }
    }
}