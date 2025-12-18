package com.mparticle.kits

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class MixpanelKitTest {

    private lateinit var kit: TestableMixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = TestableMixpanelKit()
        mockContext = mock(Context::class.java)
    }

    @Test
    fun `getName returns Mixpanel`() {
        assertEquals("Mixpanel", kit.name)
    }

    @Test
    fun `kit is not started before onKitCreate`() {
        assertFalse(kit.isStarted)
    }

    @Test
    fun `getInstance returns null before initialization`() {
        assertNull(kit.instance)
    }

    // Task 4: Token Validation Tests

    @Test(expected = IllegalArgumentException::class)
    fun `onKitCreate throws when token is missing`() {
        val settings = mutableMapOf<String, String>()
        kit.onKitCreate(settings, mockContext)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `onKitCreate throws when token is empty`() {
        val settings = mutableMapOf(MixpanelKit.KEY_TOKEN to "")
        kit.onKitCreate(settings, mockContext)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `onKitCreate throws when settings is null`() {
        kit.onKitCreate(null, mockContext)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `onKitCreate throws when context is null`() {
        val settings = mutableMapOf(MixpanelKit.KEY_TOKEN to "test-token")
        kit.onKitCreate(settings, null)
    }

    // Task 6: setOptOut Tests

    @Test
    fun `setOptOut returns empty list when not started`() {
        val result = kit.setOptOut(true)
        assertEquals(emptyList<ReportingMessage>(), result)
    }
}
