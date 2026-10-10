package io.github.athegreat90.armorstatus.error

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.util.stream.Stream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RecoveryTest {
    /** Counts how often the fallback and the failure callback were invoked. */
    private class Calls {
        var defaults = 0
        var failures = 0
    }

    @ParameterizedTest(name = "{0} is fatal")
    @MethodSource("fatalThrowables")
    fun `fatal throwables are detected`(error: Throwable) {
        assertTrue(error.isFatal())
    }

    @ParameterizedTest(name = "{0} is recoverable")
    @MethodSource("recoverableThrowables")
    fun `ordinary failures are not fatal`(error: Throwable) {
        assertFalse(error.isFatal())
    }

    @Test
    fun `rethrowFatal returns the same result for success and recoverable failure`() {
        val success = Result.success(1)
        val failure = Result.failure<Int>(IllegalStateException("bad config"))
        // Result is a value class, so compare by value rather than identity.
        assertEquals(success, success.rethrowFatal())
        assertEquals(failure, failure.rethrowFatal())
    }

    @ParameterizedTest(name = "rethrowFatal rethrows {0}")
    @MethodSource("fatalThrowables")
    fun `rethrowFatal rethrows fatal failures`(error: Throwable) {
        val thrown = assertFailsWith<Throwable> { Result.failure<Int>(error).rethrowFatal() }
        assertSame(error, thrown)
    }

    @Test
    fun `loadOrDefault returns the loaded value`() {
        val calls = Calls()
        val value =
            loadOrDefault(default = {
                calls.defaults++
                0
            }, onFailure = { calls.failures++ }) { 42 }
        assertEquals(42, value)
        assertEquals(0, calls.defaults)
        assertEquals(0, calls.failures)
    }

    @ParameterizedTest(name = "recovers from {0}")
    @MethodSource("recoverableThrowables")
    fun `loadOrDefault falls back and reports a recoverable failure exactly once`(error: Throwable) {
        val calls = Calls()
        val reported = mutableListOf<Throwable>()
        val value =
            loadOrDefault(
                default = {
                    calls.defaults++
                    -1
                },
                onFailure = {
                    calls.failures++
                    reported += it
                },
            ) { throw error }
        assertEquals(-1, value)
        assertEquals(1, calls.defaults)
        assertEquals(1, calls.failures)
        assertSame(error, reported.single())
    }

    @ParameterizedTest(name = "propagates {0}")
    @MethodSource("fatalThrowables")
    fun `loadOrDefault propagates fatal failures without touching default or onFailure`(error: Throwable) {
        val calls = Calls()
        val thrown =
            assertFailsWith<Throwable> {
                loadOrDefault(default = {
                    calls.defaults++
                    -1
                }, onFailure = { calls.failures++ }) { throw error }
            }
        assertSame(error, thrown)
        assertEquals(0, calls.defaults)
        assertEquals(0, calls.failures)
    }

    companion object {
        @JvmStatic
        fun fatalThrowables(): Stream<Throwable> =
            Stream.of(
                OutOfMemoryError("heap"),
                StackOverflowError(),
                InterruptedException("stop"),
                LinkageError("bad class"),
                NoClassDefFoundError("missing"),
                CancellationException("cancelled"),
            )

        @JvmStatic
        fun recoverableThrowables(): Stream<Throwable> =
            Stream.of(
                IllegalStateException("bad config"),
                IllegalArgumentException("bad value"),
                NumberFormatException("nan"),
                IOException("unreadable"),
                RuntimeException("generic"),
            )
    }
}
