package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.identity.MParticleUser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Integration tests that verify MixpanelKit correctly forwards calls to the Mixpanel SDK.
 * These tests use mock injection to verify SDK interactions without requiring the full
 * mParticle KitConfiguration setup.
 */
class IntegrationTest {

    private lateinit var kit: TestableMixpanelKit
    private lateinit var mockContext: Context
    private lateinit var mockMixpanel: MixpanelAPI
    private lateinit var mockPeople: MixpanelAPI.People

    @Before
    fun setUp() {
        kit = TestableMixpanelKit()
        mockContext = mock(Context::class.java)
        mockMixpanel = mock(MixpanelAPI::class.java)
        mockPeople = mock(MixpanelAPI.People::class.java)
        `when`(mockMixpanel.people).thenReturn(mockPeople)
    }

    // Initialization Tests

    @Test
    fun `initialization with valid token succeeds`() {
        kit.setMockMixpanelAPI(mockMixpanel)
        val settings = mapOf(
            KEY_TOKEN to "test-token-12345"
        )
        kit.onKitCreate(settings, mockContext)
        assertTrue(kit.isStarted)
        assertNotNull(kit.instance)
    }

    @Test
    fun `initialization parses all configuration options`() {
        kit.setMockMixpanelAPI(mockMixpanel)
        val settings = mapOf(
            KEY_TOKEN to "test-token",
            KEY_BASE_URL to "https://custom.mixpanel.com",
            KEY_USER_ID_TYPE to "MPID",
            KEY_USE_PEOPLE to "false"
        )
        kit.onKitCreate(settings, mockContext)
        assertTrue(kit.isStarted)
    }

    @Test
    fun `initialization with useMixpanelPeople true`() {
        kit.setMockMixpanelAPI(mockMixpanel)
        val settings = mapOf(
            KEY_TOKEN to "test-token",
            KEY_USE_PEOPLE to "true"
        )
        kit.onKitCreate(settings, mockContext)
        assertTrue(kit.isStarted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `initialization fails without token`() {
        kit.onKitCreate(emptyMap(), mockContext)
    }

    // Event Tracking Tests - Verify Mixpanel SDK calls

    @Test
    fun `logEvent calls Mixpanel track with event name`() {
        initializeKit()
        val event = MPEvent.Builder("Test Event", MParticle.EventType.Other).build()
        kit.logEvent(event)
        verify(mockMixpanel).track(eq("Test Event"), any())
    }

    @Test
    fun `logEvent with attributes passes properties to Mixpanel`() {
        initializeKit()
        val event = MPEvent.Builder("Test Event", MParticle.EventType.Other)
            .customAttributes(mapOf("key1" to "value1", "key2" to "value2"))
            .build()
        kit.logEvent(event)
        verify(mockMixpanel).track(eq("Test Event"), any())
    }

    @Test
    fun `logScreen calls Mixpanel track with screen name`() {
        initializeKit()
        kit.logScreen("Home Screen", mutableMapOf("section" to "main"))
        verify(mockMixpanel).track(eq("Viewed Home Screen"), any())
    }

    // Error/Exception Tests

    @Test
    fun `logError calls Mixpanel track with Error event`() {
        initializeKit()
        kit.logError("Test error message", mutableMapOf("code" to "500"))
        verify(mockMixpanel).track(eq("Error"), any())
    }

    @Test
    fun `logException calls Mixpanel track with Exception event`() {
        initializeKit()
        val exception = RuntimeException("Test exception")
        kit.logException(exception, mutableMapOf(), "Something went wrong")
        verify(mockMixpanel).track(eq("Exception"), any())
    }

    @Test
    fun `leaveBreadcrumb calls Mixpanel track with Breadcrumb event`() {
        initializeKit()
        kit.leaveBreadcrumb("User clicked checkout")
        verify(mockMixpanel).track(eq("Breadcrumb"), any())
    }

    // Commerce Tests

    @Test
    fun `commerce purchase event expands to track call with properties`() {
        initializeKit()
        val product = Product.Builder("Test Product", "SKU123", 29.99)
            .quantity(2.0)
            .build()
        val transactionAttributes = TransactionAttributes("order-123")
            .setRevenue(59.98)
        val event = CommerceEvent.Builder(Product.PURCHASE, product)
            .transactionAttributes(transactionAttributes)
            .build()
        kit.logEvent(event)
        // Commerce events are expanded to regular track() calls (trackCharge is deprecated)
        verify(mockMixpanel, atLeastOnce()).track(any<String>(), any())
    }

    @Test
    fun `commerce add to cart event expands to track call`() {
        initializeKit()
        val product = Product.Builder("Test Product", "SKU123", 29.99).build()
        val event = CommerceEvent.Builder(Product.ADD_TO_CART, product).build()
        kit.logEvent(event)
        // All commerce events are expanded to regular track() calls
        verify(mockMixpanel, atLeastOnce()).track(any<String>(), any())
    }

    // Identity Tests

    @Test
    fun `onLoginCompleted calls Mixpanel identify with customer ID`() {
        initializeKit()
        val mockUser = createMockUserWithCustomerId("user-123")
        kit.onLoginCompleted(mockUser, null)
        verify(mockMixpanel).identify("user-123")
    }

    @Test
    fun `onLogoutCompleted calls Mixpanel reset`() {
        initializeKit()
        kit.onLogoutCompleted(null, null)
        verify(mockMixpanel).reset()
    }

    @Test
    fun `onIdentifyCompleted calls Mixpanel identify`() {
        initializeKit()
        val mockUser = createMockUserWithCustomerId("user-456")
        kit.onIdentifyCompleted(mockUser, null)
        verify(mockMixpanel).identify("user-456")
    }

    @Test
    fun `onModifyCompleted calls Mixpanel identify`() {
        initializeKit()
        val mockUser = createMockUserWithCustomerId("user-789")
        kit.onModifyCompleted(mockUser, null)
        verify(mockMixpanel).identify("user-789")
    }

    @Test
    fun `onUserIdentified calls Mixpanel identify`() {
        initializeKit()
        val mockUser = createMockUserWithCustomerId("user-abc")
        kit.onUserIdentified(mockUser)
        verify(mockMixpanel).identify("user-abc")
    }

    @Test
    fun `identity methods with null user do not call identify`() {
        initializeKit()
        kit.onLoginCompleted(null, null)
        verify(mockMixpanel, never()).identify(any())
    }

    // User Attribute Tests

    @Test
    fun `onSetUserAttribute calls People set`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute("name", "John Doe", mockUser)
        verify(mockPeople).set("name", "John Doe")
    }

    @Test
    fun `onRemoveUserAttribute calls People unset`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onRemoveUserAttribute("old_key", mockUser)
        verify(mockPeople).unset("old_key")
    }

    @Test
    fun `onIncrementUserAttribute calls People increment`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onIncrementUserAttribute("login_count", 1, "5", mockUser)
        verify(mockPeople).increment("login_count", 1.0)
    }

    @Test
    fun `onSetUserTag calls People set with true value`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserTag("premium", mockUser)
        verify(mockPeople).set("premium", true)
    }

    @Test
    fun `onSetUserAttributeList calls People set with array`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttributeList("interests", mutableListOf("sports", "music"), mockUser)
        verify(mockPeople).set(eq("interests"), any())
    }

    // Reserved Attribute Mapping Tests

    @Test
    fun `onSetUserAttribute maps FirstName to first_name`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute(MParticle.UserAttributes.FIRSTNAME, "John", mockUser)
        verify(mockPeople).set("\$first_name", "John")
    }

    @Test
    fun `onSetUserAttribute maps LastName to last_name`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute(MParticle.UserAttributes.LASTNAME, "Doe", mockUser)
        verify(mockPeople).set("\$last_name", "Doe")
    }

    @Test
    fun `onSetUserAttribute maps Mobile to phone`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute(MParticle.UserAttributes.MOBILE_NUMBER, "1234-5678-90", mockUser)
        verify(mockPeople).set("\$phone", "1234-5678-90")
    }

    @Test
    fun `onSetUserAttribute maps Country to country_code`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute(MParticle.UserAttributes.COUNTRY, "USA", mockUser)
        verify(mockPeople).set("\$country_code", "USA")
    }

    @Test
    fun `onSetUserAttribute maps State to region`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute(MParticle.UserAttributes.STATE, "California", mockUser)
        verify(mockPeople).set("\$region", "California")
    }

    @Test
    fun `onSetUserAttribute maps City to city`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute(MParticle.UserAttributes.CITY, "San Francisco", mockUser)
        verify(mockPeople).set("\$city", "San Francisco")
    }

    @Test
    fun `onSetUserAttribute passes through non-reserved keys unchanged`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute("custom_attribute", "custom_value", mockUser)
        verify(mockPeople).set("custom_attribute", "custom_value")
    }

    @Test
    fun `onRemoveUserAttribute maps reserved key`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onRemoveUserAttribute(MParticle.UserAttributes.FIRSTNAME, mockUser)
        verify(mockPeople).unset("\$first_name")
    }

    @Test
    fun `onIncrementUserAttribute passes through unmapped keys`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        // AGE is not in the reserved mapping, so it passes through unchanged
        kit.onIncrementUserAttribute(MParticle.UserAttributes.AGE, 1, "30", mockUser)
        verify(mockPeople).increment(MParticle.UserAttributes.AGE, 1.0)
    }

    @Test
    fun `onSetUserAttributeList maps reserved key`() {
        initializeKit()
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttributeList(MParticle.UserAttributes.FIRSTNAME, mutableListOf("John", "Johnny"), mockUser)
        verify(mockPeople).set(eq("\$first_name"), any())
    }

    // Opt-out Tests

    @Test
    fun `setOptOut true calls optOutTracking`() {
        initializeKit()
        kit.setOptOut(true)
        verify(mockMixpanel).optOutTracking()
    }

    @Test
    fun `setOptOut false calls optInTracking`() {
        initializeKit()
        kit.setOptOut(false)
        verify(mockMixpanel).optInTracking()
    }

    // Super Properties Tests (useMixpanelPeople = false)

    @Test
    fun `onSetUserAttribute with usePeople false calls registerSuperProperties`() {
        initializeKit(usePeople = false)
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute("key", "value", mockUser)
        verify(mockMixpanel).registerSuperProperties(any())
        verify(mockPeople, never()).set(any<String>(), any())
    }

    @Test
    fun `onRemoveUserAttribute with usePeople false calls unregisterSuperProperty`() {
        initializeKit(usePeople = false)
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onRemoveUserAttribute("key", mockUser)
        verify(mockMixpanel).unregisterSuperProperty("key")
        verify(mockPeople, never()).unset(any())
    }

    @Test
    fun `onSetUserAttribute with usePeople false and reserved key calls registerSuperProperties`() {
        initializeKit(usePeople = false)
        val mockUser = mock(FilteredMParticleUser::class.java)
        kit.onSetUserAttribute(MParticle.UserAttributes.FIRSTNAME, "John", mockUser)
        verify(mockMixpanel).registerSuperProperties(any())
        verify(mockPeople, never()).set(any<String>(), any())
    }

    // MPID identity type test

    @Test
    fun `identity with MPID type uses user id`() {
        initializeKit(userIdType = "MPID")
        val mockUser = mock(MParticleUser::class.java)
        `when`(mockUser.id).thenReturn(12345L)
        `when`(mockUser.userIdentities).thenReturn(emptyMap())
        kit.onLoginCompleted(mockUser, null)
        verify(mockMixpanel).identify("12345")
    }

    // Helper methods

    private fun initializeKit(
        token: String = "test-token",
        usePeople: Boolean = true,
        userIdType: String = "CustomerId"
    ) {
        kit.setMockMixpanelAPI(mockMixpanel)
        val settings = mapOf(
            KEY_TOKEN to token,
            KEY_USE_PEOPLE to usePeople.toString(),
            KEY_USER_ID_TYPE to userIdType
        )
        kit.onKitCreate(settings, mockContext)
    }

    private fun createMockUserWithCustomerId(customerId: String): MParticleUser {
        val mockUser = mock(MParticleUser::class.java)
        val identities = mapOf(MParticle.IdentityType.CustomerId to customerId)
        `when`(mockUser.userIdentities).thenReturn(identities)
        `when`(mockUser.id).thenReturn(12345L)
        return mockUser
    }
}
