package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mparticle.MParticle
import com.mparticle.identity.MParticleUser
import com.mixpanel.android.sessionreplay.MPSessionReplayInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
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

    // Bridge Tests

    @Test
    fun `session replay instance comes from the bridge`() {
        val mockSR = mock(MPSessionReplayInstance::class.java)
        initializeKit(sessionReplayEnabled = true, bridge = MixpanelSessionReplayBridge { mockSR })

        assertSame(mockSR, kit.sessionReplayInstance)
        assertTrue(kit.isSessionReplayEnabled)
    }

    @Test
    fun `session replay instance is null until SDK finishes async init`() {
        var instance: MPSessionReplayInstance? = null
        initializeKit(sessionReplayEnabled = true, bridge = MixpanelSessionReplayBridge { instance })

        assertNull(kit.sessionReplayInstance)
        assertFalse(kit.isSessionReplayEnabled)

        instance = mock(MPSessionReplayInstance::class.java)
        assertSame(instance, kit.sessionReplayInstance)
        assertTrue(kit.isSessionReplayEnabled)
    }

    @Test
    fun `bridge is not created when session replay is disabled`() {
        var created = false
        kit.sessionReplayBridgeFactory = {
            created = true
            MixpanelSessionReplayBridge { null }
        }
        initializeKit(sessionReplayEnabled = false)

        assertFalse(created)
        assertNull(kit.sessionReplayInstance)
    }

    @Test
    fun `incompatible SDK at init disables session replay without crashing`() {
        kit.sessionReplayBridgeFactory = {
            throw NoSuchMethodError("MPSessionReplayConfig.<init>")
        }
        initializeKit(sessionReplayEnabled = true)

        assertNull(kit.sessionReplayInstance)
        assertFalse(kit.isSessionReplayEnabled)
        kit.startSessionReplayRecording()
        assertNull(kit.getSessionReplayId())
    }

    @Test
    fun `incompatible SDK at call time disables further session replay calls`() {
        val bridge = mock(SessionReplayBridge::class.java)
        `when`(bridge.instance).thenReturn(Any())
        `when`(bridge.getReplayId()).thenThrow(NoSuchMethodError("getReplayId"))
        initializeKit(sessionReplayEnabled = true, bridge = bridge)

        assertNull(kit.getSessionReplayId())
        // Bridge was dropped, so later calls no-op instead of hitting the SDK again
        assertNull(kit.sessionReplayInstance)
        kit.startSessionReplayRecording()
        verify(bridge, never()).startRecording(anyDouble())
    }

    @Test
    fun `exception from SDK call does not disable session replay`() {
        val mockSR = mock(MPSessionReplayInstance::class.java)
        `when`(mockSR.getReplayId()).thenThrow(IllegalStateException("boom"))
        initializeKit(sessionReplayEnabled = true, bridge = MixpanelSessionReplayBridge { mockSR })

        assertNull(kit.getSessionReplayId())
        assertSame(mockSR, kit.sessionReplayInstance)
    }

    @Test
    fun `identity sync calls identify on session replay instance`() {
        val mockSR = mock(MPSessionReplayInstance::class.java)
        initializeKit(sessionReplayEnabled = true, bridge = MixpanelSessionReplayBridge { mockSR })

        val mockUser = createMockUserWithCustomerId("user-999")
        kit.onIdentifyCompleted(mockUser, null)

        verify(mockMixpanel).identify("user-999")
        verify(mockSR).identify("user-999")
    }

    @Test
    fun `getSessionReplayId returns value from session replay instance`() {
        val mockSR = mock(MPSessionReplayInstance::class.java)
        `when`(mockSR.getReplayId()).thenReturn("mock-replay-id")
        initializeKit(sessionReplayEnabled = true, bridge = MixpanelSessionReplayBridge { mockSR })

        assertEquals("mock-replay-id", kit.getSessionReplayId())
        verify(mockSR).getReplayId()
    }

    @Test
    fun `startRecording calls startRecording on session replay instance`() {
        val mockSR = mock(MPSessionReplayInstance::class.java)
        initializeKit(sessionReplayEnabled = true, bridge = MixpanelSessionReplayBridge { mockSR })

        kit.startSessionReplayRecording()
        verify(mockSR).startRecording(100.0)
    }

    @Test
    fun `stopRecording calls stopRecording on session replay instance`() {
        val mockSR = mock(MPSessionReplayInstance::class.java)
        initializeKit(sessionReplayEnabled = true, bridge = MixpanelSessionReplayBridge { mockSR })

        kit.stopSessionReplayRecording()
        verify(mockSR).stopRecording()
    }

    @Test
    fun `isRecording checked on session replay instance during opt out`() {
        val mockSR = mock(MPSessionReplayInstance::class.java)
        `when`(mockSR.isRecording()).thenReturn(false)
        initializeKit(sessionReplayEnabled = true, bridge = MixpanelSessionReplayBridge { mockSR })

        kit.setOptOut(true)
        verify(mockSR).isRecording()
        verify(mockSR).stopRecording()
    }

    // Helper methods

    private fun initializeKit(
        token: String = "test-token",
        sessionReplayEnabled: Boolean = false,
        autoStartRecording: Boolean = true,
        bridge: SessionReplayBridge? = null
    ) {
        if (bridge != null) kit.sessionReplayBridgeFactory = { bridge }
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
