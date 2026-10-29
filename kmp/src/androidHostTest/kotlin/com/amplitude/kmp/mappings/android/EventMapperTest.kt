package com.amplitude.kmp.mappings.android

import com.amplitude.kmp.events.BaseEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/** Also verifies that the Android KMP host-test source set is compiled and executed. */
class EventMapperTest {
    @Test
    fun baseEventPreservesPropertiesAndOptions() {
        val event =
            BaseEvent(
                eventType = "Purchase",
                eventProperties = mutableMapOf("amount" to 29.9),
                userProperties = mutableMapOf("plan" to "premium"),
                groups = mutableMapOf("company" to "amplitude"),
            ).apply {
                userId = "test-user"
                timestamp = 1234L
            }.toAndroidBaseEvent()

        assertEquals("Purchase", event.eventType)
        assertEquals(29.9, event.eventProperties?.get("amount"))
        assertEquals("premium", event.userProperties?.get("plan"))
        assertEquals("amplitude", event.groups?.get("company"))
        assertEquals("test-user", event.userId)
        assertEquals(1234L, event.timestamp)
    }
}
