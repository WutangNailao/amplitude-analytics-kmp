package com.amplitude.kmp.mappings.ios

import com.amplitude.kmp.events.BaseEvent
import com.amplitude.kmp.events.Identify
import com.amplitude.kmp.events.Revenue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDictionary
import platform.Foundation.NSNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class EventMapperTest {
    @Test
    fun baseEventPreservesPropertyMaps() {
        val event = BaseEvent(
            eventType = "Purchase",
            userProperties = mutableMapOf("plan" to "premium"),
            groups = mutableMapOf("company" to "amplitude"),
            groupProperties = mutableMapOf("industry" to "analytics"),
        ).toIOSBaseEvent()

        assertEquals("premium", event.userProperties().get("plan"))
        assertEquals("amplitude", event.groups().get("company"))
        assertEquals("analytics", event.groupProperties().get("industry"))
    }

    @Test
    fun identifyEventPreservesNestedOperations() {
        val event = Identify().set("plan", "premium").toIdentifyEvent().toIOSBaseEvent()
        val setProperties = event.userProperties().get("\$set") as NSDictionary

        assertEquals("premium", setProperties.objectForKey("plan"))
    }

    @Test
    fun revenuePreservesCustomPropertiesAndNullValues() {
        val revenue = Revenue().apply {
            price = 29.9
            properties = mutableMapOf("source" to "settings", "optional" to null)
        }.toIOSRevenue()

        assertEquals("settings", revenue.properties().get("source"))
        assertTrue(revenue.properties().get("optional") is NSNull)
    }
}
