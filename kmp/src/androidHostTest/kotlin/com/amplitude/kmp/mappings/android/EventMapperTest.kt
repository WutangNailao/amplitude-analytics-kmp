package com.amplitude.kmp.mappings.android

import com.amplitude.kmp.events.BaseEvent
import com.amplitude.kmp.events.EventCallBack
import com.amplitude.kmp.events.IngestionMetadata
import com.amplitude.kmp.events.Plan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.amplitude.core.events.BaseEvent as AndroidBaseEvent
import com.amplitude.core.events.IngestionMetadata as AndroidIngestionMetadata
import com.amplitude.core.events.Plan as AndroidPlan

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

    @Test
    fun callbackPreservesNativeEventFieldsAndResult() {
        val native =
            AndroidBaseEvent().apply {
                eventType = "Purchase"
                eventProperties = mutableMapOf("nested" to mapOf("items" to listOf(1, null, "value")))
                userProperties = mutableMapOf("plan" to "premium")
                groups = mutableMapOf("company" to "amplitude")
                groupProperties = mutableMapOf("optional" to null)
                userId = "user"
                deviceId = "device"
                timestamp = 1234L
                eventId = 42L
                sessionId = -1L
                insertId = "insert"
                locationLat = 1.25
                locationLng = -2.5
                appVersion = "1.0"
                versionName = "release"
                platform = "Android"
                osName = "android"
                osVersion = "15"
                deviceBrand = "brand"
                deviceManufacturer = "manufacturer"
                deviceModel = "model"
                carrier = "carrier"
                country = "country"
                region = "region"
                city = "city"
                dma = "dma"
                idfa = "idfa"
                idfv = "idfv"
                adid = "adid"
                appSetId = "app-set"
                androidId = "android-id"
                language = "en"
                library = "native-library"
                ip = "127.0.0.1"
                plan = AndroidPlan("main", "mobile", "v1", "version-id")
                ingestionMetadata = AndroidIngestionMetadata("source", "source-version")
                revenue = 19.98
                price = 9.99
                quantity = 2
                productId = "product"
                revenueType = "purchase"
                currency = "USD"
                extra = mapOf("source" to "test")
                partnerId = "partner"
            }
        var returnedEvent: BaseEvent? = null
        var returnedStatus: Int? = null
        var returnedMessage: String? = null
        val callback: EventCallBack = { event, status, message ->
            returnedEvent = event
            returnedStatus = status
            returnedMessage = message
        }

        callback.toAndroidCallback()(native, 400, "invalid event")

        val event = assertNotNull(returnedEvent)
        assertEquals(400, returnedStatus)
        assertEquals("invalid event", returnedMessage)
        assertEquals(Plan("main", "mobile", "v1", "version-id"), event.plan)
        assertEquals(IngestionMetadata("source", "source-version"), event.ingestionMetadata)
        // Compare every scalar/map public getter against the native callback object.
        // Metadata uses different model classes; callback is tested separately below.
        val excluded = setOf("getClass", "getPlan", "getIngestionMetadata", "getCallback")
        for (getter in AndroidBaseEvent::class.java.methods) {
            if (!getter.name.startsWith("get") || getter.parameterCount != 0 || getter.name in excluded || '$' in getter.name) {
                continue
            }
            val kmpGetter = event.javaClass.getMethod(getter.name)
            assertEquals(getter.invoke(native), kmpGetter.invoke(event), getter.name)
        }
    }

    @Test
    fun callbackPreservesNullFieldsAndSuccessResult() {
        var calls = 0
        val callback: EventCallBack = { event, status, message ->
            calls++
            assertEquals("Empty", event.eventType)
            assertEquals(200, status)
            assertEquals("", message)
            assertNull(event.eventProperties)
            assertNull(event.userProperties)
            assertNull(event.groups)
            assertNull(event.groupProperties)
            assertNull(event.sessionId)
            assertNull(event.plan)
            assertNull(event.ingestionMetadata)
            assertNull(event.extra)
            assertNull(event.callback)
        }

        callback.toAndroidCallback()(AndroidBaseEvent().apply { eventType = "Empty" }, 200, "")

        assertEquals(1, calls)
    }

    @Test
    fun callbackPreservesCallableEventCallback() {
        var calls = 0
        val native =
            AndroidBaseEvent().apply {
                eventType = "Purchase"
                callback = { event, status, message ->
                    calls++
                    assertEquals("Follow-up", event.eventType)
                    assertEquals("follow-up-id", event.insertId)
                    assertEquals(201, status)
                    assertEquals("forwarded", message)
                }
            }
        val callback: EventCallBack = { event, _, _ ->
            val nestedCallback = assertNotNull(event.callback)
            val followUp = BaseEvent("Follow-up").apply { insertId = "follow-up-id" }
            nestedCallback(followUp, 201, "forwarded")
        }

        callback.toAndroidCallback()(native, 200, "OK")

        assertEquals(1, calls)
    }
}
