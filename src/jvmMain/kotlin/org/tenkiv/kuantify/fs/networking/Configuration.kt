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

package org.tenkiv.kuantify.fs.networking

import kotlinx.serialization.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.networking.configuration.*

@NetworkingDsl
internal inline fun <BoundT> NetworkRoute<String>.bindFS(
    serializer: KSerializer<BoundT>,
    path: Path,
    build: StringSerializingMbb<BoundT>.() -> Unit
) = bind(Serialization.json, serializer, path, build)

@NetworkingDsl
internal inline fun <BoundT> NetworkRoute<String>.bindFS(
    serializer: KSerializer<BoundT>,
    vararg path: String,
    build: StringSerializingMbb<BoundT>.() -> Unit
) = bind(Serialization.json, serializer, path.toList(), build)