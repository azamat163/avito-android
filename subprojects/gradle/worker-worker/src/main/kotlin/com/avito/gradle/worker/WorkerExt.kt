@file:Suppress("UnstableApiUsage")

package com.avito.gradle.worker

import org.gradle.internal.hash.Hasher
import org.gradle.internal.isolation.Isolatable
import org.gradle.internal.snapshot.ValueSnapshot
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

fun WorkerExecutor.inMemoryWork(work: () -> Unit) {
    noIsolation().submit(NoSerializableWork::class.java) { params ->
        params.state = NoSerializationWorkerParams.StateHolder(work)
    }
}

internal abstract class NoSerializableWork<T> : WorkAction<NoSerializationWorkerParams> {

    override fun execute() {
        parameters.state.work()
    }
}

internal abstract class NoSerializationWorkerParams : WorkParameters {

    abstract var state: StateHolder

    class StateHolder(val work: () -> Unit) : Isolatable<StateHolder> {

        override fun <S : Any?> coerce(type: Class<S>): S? {
            return null
        }

        override fun isolate(): StateHolder {
            return this
        }

        override fun appendToHasher(hasher: Hasher) {
            throw UnsupportedOperationException()
        }

        override fun asSnapshot(): ValueSnapshot {
            throw UnsupportedOperationException()
        }
    }
}