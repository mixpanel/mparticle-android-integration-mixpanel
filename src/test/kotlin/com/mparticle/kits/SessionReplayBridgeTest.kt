package com.mparticle.kits

import com.mixpanel.android.sessionreplay.sensitive_views.AutoMaskedView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs against the real Session Replay SDK. CI repeats these tests against several SDK
 * versions (-PsessionReplayTestVersion) to catch binary incompatibilities like SDK-172.
 */
class SessionReplayBridgeTest {

    @Test
    fun `buildConfig maps kit configuration onto SDK config`() {
        val config = MixpanelSessionReplayBridge.buildConfig(
            SessionReplayConfiguration(
                enabled = true,
                recordSessionsPercent = 25.0,
                autoStartRecording = false,
                wifiOnly = false,
                enableLogging = true,
                flushIntervalSeconds = 30
            )
        )

        assertFalse(config.wifiOnly)
        assertEquals(30L, config.flushInterval)
        assertFalse(config.autoStartRecording)
        assertEquals(25.0, config.recordingSessionsPercent, 0.0)
        assertTrue(config.enableLogging)
    }

    @Test
    fun `buildConfig masks all view types by default`() {
        val config = MixpanelSessionReplayBridge.buildConfig(SessionReplayConfiguration())

        assertEquals(
            setOf(AutoMaskedView.Text, AutoMaskedView.Image, AutoMaskedView.Web),
            config.autoMaskedViews
        )
    }

    @Test
    fun `buildConfig only masks enabled view types`() {
        val config = MixpanelSessionReplayBridge.buildConfig(
            SessionReplayConfiguration(maskText = true, maskImages = false, maskWebViews = false)
        )

        assertEquals(setOf(AutoMaskedView.Text), config.autoMaskedViews)
    }

    @Test
    fun `buildConfig with no masks produces empty set`() {
        val config = MixpanelSessionReplayBridge.buildConfig(
            SessionReplayConfiguration(maskText = false, maskImages = false, maskWebViews = false)
        )

        assertTrue(config.autoMaskedViews.isEmpty())
    }

    @Test
    fun `buildConfig matches SDK defaults for kit defaults`() {
        val kitDefault = MixpanelSessionReplayBridge.buildConfig(SessionReplayConfiguration())
        val sdkDefault = SessionReplayConfigFactory.create()

        assertEquals(sdkDefault.wifiOnly, kitDefault.wifiOnly)
        assertEquals(sdkDefault.autoStartRecording, kitDefault.autoStartRecording)
        assertEquals(sdkDefault.recordingSessionsPercent, kitDefault.recordingSessionsPercent, 0.0)
        assertEquals(sdkDefault.enableLogging, kitDefault.enableLogging)
    }

    @Test
    fun `default bridge resolves instance from SDK`() {
        // The SDK has not been initialized, so getInstance() returns null
        assertNull(MixpanelSessionReplayBridge().instance)
    }
}
