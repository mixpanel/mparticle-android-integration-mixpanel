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

    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = MixpanelKit()
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

    // Note: buildCommerceEventProperties is tested indirectly through IntegrationTest
    // which verifies commerce events are properly forwarded to Mixpanel with all properties.
    // Direct unit testing of buildCommerceEventProperties is complex due to mParticle SDK
    // class mocking requirements. The method's behavior matches the iOS implementation:
    // - Includes expanded event attributes
    // - Includes commerce event custom attributes (overrides expanded if same key)
    // - Includes transaction attributes: Revenue, Transaction Id, Tax, Shipping, Coupon Code
}
