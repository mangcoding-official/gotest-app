package com.gotest.app.core.data.fake

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlin.random.Random

private const val MIN_LATENCY_MS = 300L
private const val MAX_LATENCY_MS = 500L

/** Pretends to hit the network so screens show their loading state. */
suspend fun simulateLatency() = delay(Random.nextLong(MIN_LATENCY_MS, MAX_LATENCY_MS))

/** Delays the first emission of a fake stream by [simulateLatency]. */
fun <T> Flow<T>.withFakeLatency(): Flow<T> = onStart { simulateLatency() }

/** Coil model for a bundled drawable, so dummy data can stand in for remote image URLs. */
fun drawableUri(name: String): String = "android.resource://com.gotest.app/drawable/$name"
