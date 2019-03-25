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

package org.tenkiv.kuantify.android.gate

import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*

public interface AndroidDaqcGate<T : DaqcData> : DaqcGate<T> {

    public val uid: String

}

public object AndroidGateTypeId {
    public const val AMBIENT_TEMPERATURE: String = "AT"
    public const val HEART_RATE: String = "HR"
    public const val LIGHT: String = "LI"
    public const val PROXIMITY: String = "PX"
    public const val PRESSURE: String = "PS"
    public const val RELATIVE_HUMIDITY: String = "HU"
    public const val TORCH: String = "TO"
}