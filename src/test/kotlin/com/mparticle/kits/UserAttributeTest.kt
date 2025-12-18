package com.mparticle.kits

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class UserAttributeTest {

    private lateinit var kit: TestableMixpanelKit
    private lateinit var mockContext: Context
    private lateinit var mockUser: FilteredMParticleUser

    @Before
    fun setUp() {
        kit = TestableMixpanelKit()
        mockContext = mock(Context::class.java)
        mockUser = mock(FilteredMParticleUser::class.java)
    }

    // These tests verify methods don't throw when not started

    @Test
    fun `onSetUserAttribute does not throw when not started`() {
        kit.onSetUserAttribute("key", "value", mockUser)
        // No exception means success
    }

    @Test
    fun `onRemoveUserAttribute does not throw when not started`() {
        kit.onRemoveUserAttribute("key", mockUser)
    }

    @Test
    fun `onIncrementUserAttribute does not throw when not started`() {
        kit.onIncrementUserAttribute("key", 1.0, "1.0", mockUser)
    }

    @Test
    fun `onSetUserTag does not throw when not started`() {
        kit.onSetUserTag("tag", mockUser)
    }

    @Test
    fun `onSetUserAttributeList does not throw when not started`() {
        kit.onSetUserAttributeList("key", mutableListOf("a", "b", "c"), mockUser)
    }

    @Test
    fun `onSetAllUserAttributes does not throw when not started`() {
        kit.onSetAllUserAttributes(
            mutableMapOf("key1" to "value1"),
            mutableMapOf("key2" to mutableListOf("a", "b")),
            mockUser
        )
    }

    @Test
    fun `supportsAttributeLists returns true`() {
        assertTrue(kit.supportsAttributeLists())
    }

    @Test
    fun `onConsentStateUpdated does not throw`() {
        kit.onConsentStateUpdated(null, null, mockUser)
    }

    // Null/empty key validation tests

    @Test
    fun `onSetUserAttribute handles null key`() {
        kit.onSetUserAttribute(null, "value", mockUser)
    }

    @Test
    fun `onSetUserAttribute handles empty key`() {
        kit.onSetUserAttribute("", "value", mockUser)
    }

    @Test
    fun `onSetUserAttribute handles null value`() {
        kit.onSetUserAttribute("key", null, mockUser)
    }

    @Test
    fun `onRemoveUserAttribute handles null key`() {
        kit.onRemoveUserAttribute(null, mockUser)
    }

    @Test
    fun `onSetUserAttributeList handles null values`() {
        kit.onSetUserAttributeList("key", null, mockUser)
    }

    @Test
    fun `onSetUserAttributeList handles empty values`() {
        kit.onSetUserAttributeList("key", mutableListOf(), mockUser)
    }
}
