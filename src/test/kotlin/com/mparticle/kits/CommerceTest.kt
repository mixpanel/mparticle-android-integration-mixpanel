package com.mparticle.kits

import android.content.Context
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class CommerceTest {

    private lateinit var kit: TestableMixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = TestableMixpanelKit()
        mockContext = mock(Context::class.java)
    }

    @Test
    fun `logEvent commerce returns null when not started`() {
        val product = Product.Builder("Test Product", "SKU123", 9.99).build()
        val event = CommerceEvent.Builder(Product.PURCHASE, product).build()
        val result = kit.logEvent(event)
        assertNull(result)
    }

    @Test
    fun `logLtvIncrease returns null`() {
        val result = kit.logLtvIncrease(
            java.math.BigDecimal("10.00"),
            java.math.BigDecimal("100.00"),
            "Test LTV",
            mutableMapOf()
        )
        assertNull(result)
    }

    @Test
    fun `supportsAttributeLists returns true`() {
        assertTrue(kit.supportsAttributeLists())
    }
}
