package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mparticle.MParticle
import com.mparticle.identity.MParticleUser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Tests for Session Replay behavior including:
 * - Graceful handling when SDK is absent
 * - Opt-out stopping recording
 * - Identity sync with Session Replay
 */
class SessionReplayBehaviorTest {

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
        `when`(mockMixpanel.distinctId).thenReturn("test-distinct-id")
    }

    // SDK Absent Tests

    @Test
    fun `session replay enabled with SDK absent does not crash`() {
        // Initialize kit with Session Replay enabled but SDK not available
        initializeKit(sessionReplayEnabled = true)
        // Should not throw - graceful degradation
    }

    @Test
    fun `session replay enabled with SDK absent returns isSessionReplayEnabled false`() {
        initializeKit(sessionReplayEnabled = true)
        // SDK is not on classpath, so isSessionReplayEnabled should be false
        assertFalse(kit.isSessionReplayEnabled)
    }

    @Test
    fun `session replay instance is null when SDK absent`() {
        initializeKit(sessionReplayEnabled = true)
        assertNull(kit.sessionReplayInstance)
    }

    @Test
    fun `start recording with SDK absent does not crash`() {
        initializeKit(sessionReplayEnabled = true)
        kit.startSessionReplayRecording()
        // Should not throw
    }

    @Test
    fun `stop recording with SDK absent does not crash`() {
        initializeKit(sessionReplayEnabled = true)
        kit.stopSessionReplayRecording()
        // Should not throw
    }

    @Test
    fun `get replay ID with SDK absent returns null`() {
        initializeKit(sessionReplayEnabled = true)
        val replayId = kit.getSessionReplayId()
        assertNull(replayId)
    }

    // Opt-out Tests

    @Test
    fun `opt out calls optOutTracking on Mixpanel`() {
        initializeKit()
        kit.setOptOut(true)
        verify(mockMixpanel).optOutTracking()
    }

    @Test
    fun `opt in calls optInTracking on Mixpanel`() {
        initializeKit()
        kit.setOptOut(false)
        verify(mockMixpanel).optInTracking()
    }

    @Test
    fun `opt out with session replay enabled does not crash`() {
        initializeKit(sessionReplayEnabled = true)
        kit.setOptOut(true)
        // Should not throw - stopSessionReplayRecording handles null instance gracefully
    }

    @Test
    fun `opt in with session replay enabled and autostart does not crash`() {
        initializeKit(sessionReplayEnabled = true)
        kit.setOptOut(false)
        // Should not throw - startSessionReplayRecording handles null instance gracefully
    }

    // Identity Sync Tests

    @Test
    fun `identify calls Mixpanel identify`() {
        initializeKit()
        val mockUser = createMockUserWithCustomerId("user-123")
        kit.onIdentifyCompleted(mockUser, null)
        verify(mockMixpanel).identify("user-123")
    }

    @Test
    fun `login calls Mixpanel identify`() {
        initializeKit()
        val mockUser = createMockUserWithCustomerId("user-456")
        kit.onLoginCompleted(mockUser, null)
        verify(mockMixpanel).identify("user-456")
    }

    @Test
    fun `identity sync with session replay enabled and SDK absent does not crash`() {
        initializeKit(sessionReplayEnabled = true)
        val mockUser = createMockUserWithCustomerId("user-789")
        kit.onIdentifyCompleted(mockUser, null)
        // Should not throw - identifySessionReplay handles null instance gracefully
        verify(mockMixpanel).identify("user-789")
    }

    // Helper methods

    private fun initializeKit(
        token: String = "test-token",
        sessionReplayEnabled: Boolean = false
    ) {
        kit.setMockMixpanelAPI(mockMixpanel)
        val settings = mutableMapOf(
            KEY_TOKEN to token,
            KEY_USE_PEOPLE to "true"
        )
        if (sessionReplayEnabled) {
            settings[KEY_SESSION_REPLAY_ENABLED] = "true"
        }
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
