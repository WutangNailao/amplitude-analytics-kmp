package com.amplitude.kmp.mappings.android

import com.amplitude.kmp.events.Revenue
import com.amplitude.core.events.Revenue as AndroidRevenue

/**
 * Map KMP Revenue to Android Revenue.
 */
internal fun Revenue.toAndroidRevenue(): AndroidRevenue {
    return AndroidRevenue().apply {
        this@toAndroidRevenue.productId?.let { this.productId = it }
        this.quantity = this@toAndroidRevenue.quantity
        this@toAndroidRevenue.price?.let { this.price = it }
        this@toAndroidRevenue.revenueType?.let { this.revenueType = it }
        this@toAndroidRevenue.currency?.let { this.currency = it }
        this@toAndroidRevenue.revenue?.let { this.revenue = it }

        this.receipt = this@toAndroidRevenue.receipt
        this.receiptSig = this@toAndroidRevenue.receiptSig

        this@toAndroidRevenue.properties?.let {
            if (it.isNotEmpty()) {
                this.properties = it
            }
        }
    }
}
