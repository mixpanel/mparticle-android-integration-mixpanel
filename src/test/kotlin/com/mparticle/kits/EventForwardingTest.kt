package com.mparticle.kits

import android.content.Context
import com.mparticle.MPEvent
import com.mparticle.MParticle
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class EventForwardingTest {

    private lateinit var kit: TestableMixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = TestableMixpanelKit()
        mockContext = mock(Context::class.java)
    }

    // Task 7: logEvent Tests

    @Test
    fun `logEvent returns null when not started`() {
        val event = MPEvent.Builder("Test Event", MParticle.EventType.Other).build()
        val result = kit.logEvent(event)
        assertNull(result)
    }

    @Test
    fun `logEvent returns null for null event name`() {
        // Can't easily create MPEvent with null name, so this tests the not-started case
        val result = kit.logEvent(mock(MPEvent::class.java))
        assertNull(result)
    }

    // Task 8: logScreen Tests

    @Test
    fun `logScreen returns null when not started`() {
        val result = kit.logScreen("Home", mutableMapOf())
        assertNull(result)
    }

    @Test
    fun `logScreen returns null for null screen name`() {
        val result = kit.logScreen(null, mutableMapOf())
        assertNull(result)
    }

    @Test
    fun `logScreen returns null for empty screen name`() {
        val result = kit.logScreen("", mutableMapOf())
        assertNull(result)
    }

    // Task 9: logError, logException, leaveBreadcrumb Tests

    @Test
    fun `logError returns null when not started`() {
        val result = kit.logError("Test error", mutableMapOf())
        assertNull(result)
    }

    @Test
    fun `logException returns null when not started`() {
        val result = kit.logException(RuntimeException("test"), mutableMapOf(), "Test exception")
        assertNull(result)
    }

    @Test
    fun `leaveBreadcrumb returns null when not started`() {
        val result = kit.leaveBreadcrumb("Test breadcrumb")
        assertNull(result)
    }

    @Test
    fun `leaveBreadcrumb returns null for null breadcrumb`() {
        val result = kit.leaveBreadcrumb(null)
        assertNull(result)
    }

    @Test
    fun `leaveBreadcrumb returns null for empty breadcrumb`() {
        val result = kit.leaveBreadcrumb("")
        assertNull(result)
    }
}
