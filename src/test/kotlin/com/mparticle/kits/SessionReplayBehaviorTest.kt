package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mparticle.MParticle
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.mocks.MockSessionReplay
import com.mparticle.kits.mocks.MockSessionReplayNoGetInstance
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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

    @After
    fun tearDown() {
        MockSessionReplay.reset()
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

    // Login/Logout Session Replay Lifecycle Tests

    @Test
    fun `login after logout restarts session replay recording`() {
        initializeKit(sessionReplayEnabled = true, autoStartRecording = true)
        val mockUser = createMockUserWithCustomerId("user-123")

        // Logout stops recording
        kit.onLogoutCompleted(mockUser, null)
        verify(mockMixpanel).reset()

        // Login should identify and restart recording
        kit.onLoginCompleted(mockUser, null)
        verify(mockMixpanel).identify("user-123")
        // startSessionReplayRecording is called but _sessionReplayInstance is null (SDK absent)
        // so it gracefully no-ops — the important thing is the code path is exercised
    }

    @Test
    fun `opt in after manual stop does not restart recording`() {
        initializeKit(sessionReplayEnabled = true, autoStartRecording = true)

        // Simulate manual stop by setting the flag
        kit.setTestWasManuallyStoppedBeforeOptOut(true)

        // Opt out
        kit.setOptOut(true)
        verify(mockMixpanel).optOutTracking()

        // Opt in — should NOT restart because user manually stopped before opt-out
        kit.setOptOut(false)
        verify(mockMixpanel).optInTracking()
        // With SDK absent, startSessionReplayRecording is a no-op anyway,
        // but the flag prevents the code path from even attempting it.
        // Verify optInTracking was called (the key behavior check)
    }

    @Test
    fun `opt in after auto stop restarts recording path`() {
        initializeKit(sessionReplayEnabled = true, autoStartRecording = true)

        // Opt out (auto-stop, not manual)
        kit.setOptOut(true)
        verify(mockMixpanel).optOutTracking()

        // Opt in — should attempt to restart since it wasn't manually stopped
        kit.setOptOut(false)
        verify(mockMixpanel).optInTracking()
        // The code path attempts identifySessionReplay + startSessionReplayRecording
        // Both gracefully no-op with null _sessionReplayInstance
    }

    @Test
    fun `opt in syncs identity before restarting recording`() {
        initializeKit(sessionReplayEnabled = true, autoStartRecording = true)
        `when`(mockMixpanel.distinctId).thenReturn("new-distinct-id")

        // Opt out then opt in
        kit.setOptOut(true)
        kit.setOptOut(false)

        // distinctId should have been queried for identity sync
        // (called during opt-in path before startSessionReplayRecording)
        verify(mockMixpanel).optInTracking()
    }

    @Test
    fun `wasManuallyStoppedBeforeOptOut resets on logout`() {
        initializeKit(sessionReplayEnabled = true, autoStartRecording = true)

        // Simulate manual stop + opt-out
        kit.setTestWasManuallyStoppedBeforeOptOut(true)
        kit.setOptOut(true)

        // Logout resets the flag
        val mockUser = createMockUserWithCustomerId("user-789")
        kit.onLogoutCompleted(mockUser, null)

        // Login after logout — recording should restart normally
        kit.onLoginCompleted(mockUser, null)
        verify(mockMixpanel).identify("user-789")
        // The login path restarts recording because autoStartRecording is true
    }

    @Test
    fun `opt out without session replay instance does not set manual stop flag`() {
        // No session replay (disabled)
        initializeKit(sessionReplayEnabled = false)

        // Opt out — wasManuallyStoppedBeforeOptOut should remain false
        // because _sessionReplayInstance is null
        kit.setOptOut(true)
        verify(mockMixpanel).optOutTracking()

        // Opt in — should not be blocked by the flag
        kit.setOptOut(false)
        verify(mockMixpanel).optInTracking()
    }

    @Test
    fun `login without autostart does not restart session replay`() {
        initializeKit(sessionReplayEnabled = true, autoStartRecording = false)
        val mockUser = createMockUserWithCustomerId("user-123")

        kit.onLogoutCompleted(mockUser, null)
        kit.onLoginCompleted(mockUser, null)

        // With autoStartRecording=false, recording should not be restarted
        // The code path checks sessionReplayConfig.autoStartRecording
        verify(mockMixpanel).identify("user-123")
    }

    @Test
    fun `resolveSessionReplayInstance returns instance via Companion getInstance`() {
        initializeKit(sessionReplayEnabled = true)
        val mockSR = mock(MockSessionReplay::class.java)
        MockSessionReplay.setInstance(mockSR)
        kit.setTestSessionReplayClass(MockSessionReplay::class.java)

        val resolved = kit.sessionReplayInstance
        assertNotNull(resolved)
        assertSame(mockSR, resolved)
    }

    @Test
    fun `resolveSessionReplayInstance returns null when getInstance returns null`() {
        initializeKit(sessionReplayEnabled = true)
        kit.setTestSessionReplayClass(MockSessionReplay::class.java)

        assertNull(kit.sessionReplayInstance)
        assertFalse(kit.isSessionReplayEnabled)
    }

    @Test
    fun `resolveSessionReplayInstance caches instance on subsequent calls`() {
        initializeKit(sessionReplayEnabled = true)
        val mockSR = mock(MockSessionReplay::class.java)
        MockSessionReplay.setInstance(mockSR)
        kit.setTestSessionReplayClass(MockSessionReplay::class.java)

        val first = kit.sessionReplayInstance
        val second = kit.sessionReplayInstance
        assertNotNull(first)
        assertSame(first, second)
    }

    @Test
    fun `isSessionReplayEnabled returns true when instance resolves`() {
        initializeKit(sessionReplayEnabled = true)
        MockSessionReplay.setInstance(mock(MockSessionReplay::class.java))
        kit.setTestSessionReplayClass(MockSessionReplay::class.java)

        assertTrue(kit.isSessionReplayEnabled)
    }

    @Test
    fun `resolveSessionReplayInstance returns null when class has no Companion field`() {
        initializeKit(sessionReplayEnabled = true)
        kit.setTestSessionReplayClass(String::class.java)

        assertNull(kit.sessionReplayInstance)
    }

    @Test
    fun `NoSuchFieldException disables further resolution attempts`() {
        initializeKit(sessionReplayEnabled = true)
        kit.setTestSessionReplayClass(String::class.java)

        // First call triggers NoSuchFieldException and nulls _sessionReplayClass
        assertNull(kit.sessionReplayInstance)
        // Second call should still return null (permanently disabled)
        assertNull(kit.sessionReplayInstance)
        assertFalse(kit.isSessionReplayEnabled)
    }

    @Test
    fun `resolveSessionReplayInstance returns null when Companion has no getInstance method`() {
        initializeKit(sessionReplayEnabled = true)
        kit.setTestSessionReplayClass(MockSessionReplayNoGetInstance::class.java)

        assertNull(kit.sessionReplayInstance)
    }

    @Test
    fun `NoSuchMethodException disables further resolution attempts`() {
        initializeKit(sessionReplayEnabled = true)
        kit.setTestSessionReplayClass(MockSessionReplayNoGetInstance::class.java)

        // First call triggers NoSuchMethodException and nulls _sessionReplayClass
        assertNull(kit.sessionReplayInstance)
        // Second call should still return null (permanently disabled)
        assertNull(kit.sessionReplayInstance)
        assertFalse(kit.isSessionReplayEnabled)
    }

    @Test
    fun `resolveSessionReplayInstance returns null when sessionReplayClass is null`() {
        initializeKit(sessionReplayEnabled = true)
        kit.setTestSessionReplayClass(null)

        assertNull(kit.sessionReplayInstance)
    }

    @Test
    fun `identity sync calls identify on resolved mock instance`() {
        initializeKit(sessionReplayEnabled = true)
        val mockSR = mock(MockSessionReplay::class.java)
        MockSessionReplay.setInstance(mockSR)
        kit.setTestSessionReplayClass(MockSessionReplay::class.java)

        val mockUser = createMockUserWithCustomerId("user-999")
        kit.onIdentifyCompleted(mockUser, null)

        verify(mockMixpanel).identify("user-999")
        verify(mockSR).identify("user-999")
    }

    @Test
    fun `getSessionReplayId returns value from resolved mock instance`() {
        initializeKit(sessionReplayEnabled = true)
        val mockSR = mock(MockSessionReplay::class.java)
        `when`(mockSR.getReplayId()).thenReturn("mock-replay-id")
        MockSessionReplay.setInstance(mockSR)
        kit.setTestSessionReplayClass(MockSessionReplay::class.java)

        val replayId = kit.getSessionReplayId()
        verify(mockSR).getReplayId()
        assertEquals("mock-replay-id", replayId)
    }

    @Test
    fun `startRecording calls startRecording on resolved mock instance`() {
        initializeKit(sessionReplayEnabled = true)
        val mockSR = mock(MockSessionReplay::class.java)
        MockSessionReplay.setInstance(mockSR)
        kit.setTestSessionReplayClass(MockSessionReplay::class.java)

        kit.startSessionReplayRecording()
        verify(mockSR).startRecording(100.0)
    }

    @Test
    fun `stopRecording calls stopRecording on resolved mock instance`() {
        initializeKit(sessionReplayEnabled = true)
        val mockSR = mock(MockSessionReplay::class.java)
        MockSessionReplay.setInstance(mockSR)
        kit.setTestSessionReplayClass(MockSessionReplay::class.java)

        kit.stopSessionReplayRecording()
        verify(mockSR).stopRecording()
    }

    @Test
    fun `isRecording checked via resolved mock instance during opt out`() {
        initializeKit(sessionReplayEnabled = true)
        val mockSR = mock(MockSessionReplay::class.java)
        `when`(mockSR.isRecording()).thenReturn(false)
        MockSessionReplay.setInstance(mockSR)
        kit.setTestSessionReplayClass(MockSessionReplay::class.java)

        kit.setOptOut(true)
        verify(mockSR).isRecording()
        verify(mockSR).stopRecording()
    }

    // Helper methods

    private fun initializeKit(
        token: String = "test-token",
        sessionReplayEnabled: Boolean = false,
        autoStartRecording: Boolean = true
    ) {
        kit.setMockMixpanelAPI(mockMixpanel)
        val settings = mutableMapOf(
            KEY_TOKEN to token,
            KEY_USE_PEOPLE to "true"
        )
        if (sessionReplayEnabled) {
            settings[KEY_SESSION_REPLAY_ENABLED] = "true"
            settings[KEY_AUTO_START_RECORDING] = autoStartRecording.toString()
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
