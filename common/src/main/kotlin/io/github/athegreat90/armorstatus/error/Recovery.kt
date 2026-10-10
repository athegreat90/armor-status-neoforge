package io.github.athegreat90.armorstatus.error

import kotlin.coroutines.cancellation.CancellationException

/**
 * True for failures that must never be swallowed: cancellation, thread interruption, JVM-level errors
 * (out of memory, stack overflow) and class linkage problems. Everything else is recoverable.
 */
fun Throwable.isFatal(): Boolean =
    this is CancellationException ||
        this is InterruptedException ||
        this is VirtualMachineError ||
        this is LinkageError

/** Rethrows the failure if it [isFatal]; otherwise returns this result unchanged. */
fun <T> Result<T>.rethrowFatal(): Result<T> {
    val failure = exceptionOrNull()
    if (failure != null && failure.isFatal()) throw failure
    return this
}

/**
 * Runs [load] at a recoverable boundary (for example reading the config file). A recoverable failure is
 * passed to [onFailure] once and replaced by [default]; a fatal one (see [isFatal]) propagates and neither
 * callback runs. This is the only place the mod uses [runCatching]; never call it in the render path.
 */
inline fun <T> loadOrDefault(default: () -> T, onFailure: (Throwable) -> Unit, load: () -> T): T =
    runCatching(load).rethrowFatal().onFailure(onFailure).getOrElse { default() }
