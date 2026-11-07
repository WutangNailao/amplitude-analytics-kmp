package com.amplitude.kmp.mappings.ios

import com.amplitude.kmp.Configuration
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalForeignApi::class)
class ConfigurationMapperTest {
    @Test
    fun configurationPreservesDefaultDurations() {
        val configuration = Configuration(apiKey = "test-api-key")
        val native = configuration.toIOSConfiguration()

        assertEquals(configuration.minTimeBetweenSessionsMillis, native.minTimeBetweenSessionsMillis())
        assertEquals(configuration.identifyBatchIntervalMillis, native.identifyBatchIntervalMillis())
    }

    @Test
    fun configurationPreservesDurationsAtAndAboveIntLimit() {
        val durations =
            listOf(
                Int.MAX_VALUE.toLong(),
                Int.MAX_VALUE.toLong() + 1L,
                30L * 24 * 60 * 60 * 1000,
                1L shl 32,
            )

        for (duration in durations) {
            val native =
                Configuration(
                    apiKey = "test-api-key",
                    minTimeBetweenSessionsMillis = duration,
                    identifyBatchIntervalMillis = duration,
                ).toIOSConfiguration()

            assertEquals(duration, native.minTimeBetweenSessionsMillis(), "session duration=$duration")
            assertEquals(duration, native.identifyBatchIntervalMillis(), "identify duration=$duration")
        }
    }
}
