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

package kuantify.gate.acquire

public sealed class ProcessFailure {
    public abstract val acquireGate: AcquireChannel<*>
    public abstract val message: String
    public abstract val cause: Throwable?

    public class IllegalDependencyState internal constructor(
        public override val acquireGate: AcquireChannel<*>,
        public override val message: String,
        public override val cause: Throwable?
    ) : ProcessFailure()

    public class OutOfRange(
        public override val acquireGate: AcquireChannel<*>,
        public override val message: String,
        public override val cause: Throwable?
    ) : ProcessFailure()

}

public fun AcquireChannel<*>.IllegalDependencyState(
    message: String,
    cause: Throwable? = null
): ProcessFailure.IllegalDependencyState =
    ProcessFailure.IllegalDependencyState(this, message, cause)

public fun AcquireChannel<*>.OutOfRange(
    message: String,
    cause: Throwable? = null
): ProcessFailure.OutOfRange =
    ProcessFailure.OutOfRange(this, message, cause)

public fun ProcessFailure.throwException(): Nothing = throw ProcessFailureException(
    this
)

public class ProcessFailureException(processFailure: ProcessFailure) : Throwable(
    cause = processFailure.cause,
    message = processFailure.message
)