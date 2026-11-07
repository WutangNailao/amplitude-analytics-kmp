package com.amplitude.kmp.mappings.android

import com.amplitude.kmp.events.Revenue
import kotlin.test.Test
import kotlin.test.assertEquals

class RevenueMapperTest {
    @Test
    fun revenuePreservesReceiptAndSignatureIndependently() {
        for (receipt in listOf(null, "receipt-data")) {
            for (signature in listOf(null, "signature-data")) {
                val native =
                    Revenue().apply {
                        price = 9.99
                        this.receipt = receipt
                        receiptSig = signature
                    }.toAndroidRevenue()

                assertEquals(receipt, native.receipt, "receipt=$receipt, signature=$signature")
                assertEquals(signature, native.receiptSig, "receipt=$receipt, signature=$signature")
                assertEquals(receipt, native.toRevenueEvent().eventProperties?.get("\$receipt"))
                assertEquals(signature, native.toRevenueEvent().eventProperties?.get("\$receiptSig"))
            }
        }
    }
}
