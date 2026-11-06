package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.sessionreplay.MPSessionReplay
import com.mixpanel.android.sessionreplay.models.MPSessionReplayConfig
import com.mixpanel.android.sessionreplay.sensitive_views.AutoMaskedView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`

/**
 * Runs the kit's Session Replay initialization against the real Session Replay SDK, with
 * [MPSessionReplay.initialize] stubbed out, and checks the config the kit passes to it.
 */
class SessionReplayInitializationTest {

    @Test
    fun `initialization passes MPSessionReplayConfig built from kit settings`() {
        val kit = object : MixpanelKit() {
            fun configure(config: SessionReplayConfiguration) = setSessionReplayConfig(config)
        }
        kit.configure(
            SessionReplayConfiguration(
                enabled = true,
                recordSessionsPercent = 25.0,
                autoStartRecording = false,
                wifiOnly = false,
                maskImages = false,
                maskText = true,
                maskWebViews = true,
                enableLogging = true,
                flushIntervalSeconds = 30
            )
        )
        val context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(context)

        // The kit invokes the static initialize(Context, String, String, MPSessionReplayConfig)
        // reflectively; Kotlin calls would go through the companion object instead.
        val initialize = MPSessionReplay::class.java.getMethod(
            "initialize",
            Context::class.java,
            String::class.java,
            String::class.java,
            MPSessionReplayConfig::class.java
        )
        val initializeSessionReplay = MixpanelKit::class.java.getDeclaredMethod(
            "initializeSessionReplayIfEnabled",
            Context::class.java,
            String::class.java
        ).apply { isAccessible = true }

        mockStatic(MPSessionReplay::class.java).use { sessionReplay ->
            initializeSessionReplay.invoke(kit, context, "test-token")

            val captor = ArgumentCaptor.forClass(MPSessionReplayConfig::class.java)
            sessionReplay.verify {
                initialize.invoke(null, any(Context::class.java), anyString(), anyString(), captor.capture())
            }
            val config = captor.value
            assertEquals(false, config.wifiOnly)
            assertEquals(30L, config.flushInterval)
            assertEquals(false, config.autoStartRecording)
            assertEquals(25.0, config.recordingSessionsPercent, 0.0)
            assertEquals(setOf(AutoMaskedView.Text, AutoMaskedView.Web), config.autoMaskedViews)
            assertEquals(true, config.enableLogging)
        }
    }
}
