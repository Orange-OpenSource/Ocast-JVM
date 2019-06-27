/*
 * Copyright 2019 Orange
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ocast.common.utils

import org.ocast.core.models.CallbackWrapper
import org.ocast.core.models.Consumer

/**
 * This interface is implemented by classes which have a callback wrapper.
 */
internal interface CallbackWrapperOwner {

    /** The callback wrapper. */
    var callbackWrapper: CallbackWrapper

    /**
     * Wraps and runs the lambda.
     *
     * @param param The parameter passed to the run method of the wrapped lambda.
     */
    fun <T> ((T) -> Unit).wrapRun(param: T) {
        callbackWrapper.wrap(this).run(param)
    }

    /**
     * Wraps and runs the [Consumer].
     *
     * @param param The parameter passed to the run method of the wrapped [Consumer].
     */
    fun <T> Consumer<T>.wrapRun(param: T) {
        callbackWrapper.wrap(this).run(param)
    }

    /**
     * Wraps and runs the [Runnable].
     */
    fun Runnable.wrapRun() {
        callbackWrapper.wrap(this).run()
    }

    /**
     * Wraps and performs the given [action] for each element in the sequence.
     *
     * @param action The lambda to wrap and perform.
     */
    fun <T> Iterable<T>.wrapForEach(action: (T) -> Unit) {
        forEach { action.wrapRun(it) }
    }
}
