package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.sessionreplay.MPSessionReplay
import com.mixpanel.android.sessionreplay.MPSessionReplayInstance
import com.mixpanel.android.sessionreplay.models.MPSessionReplayConfig
import com.mixpanel.android.sessionreplay.sensitive_views.AutoMaskedView

/**
 * The kit's view of the optional Mixpanel Session Replay SDK.
 *
 * [MixpanelKit] only talks to Session Replay through this interface, so the kit itself never
 * references Session Replay types and loads fine when the SDK is not on the classpath.
 */
internal interface SessionReplayBridge {
    /** The Session Replay SDK instance, or null until its async initialization completes. */
    val instance: Any?
    fun identify(distinctId: String)
    fun isRecording(): Boolean
    fun startRecording(sessionsPercent: Double)
    fun stopRecording()
    fun getReplayId(): String?
}

/**
 * [SessionReplayBridge] backed by the Session Replay SDK's public API.
 *
 * This is the only class that references Session Replay types. It is compiled against the
 * oldest supported SDK version and must only use API that is stable across releases. In
 * particular, never call the [MPSessionReplayConfig] constructor from Kotlin (even with no
 * arguments) or use `copy()`: its parameter list grows between releases, which changes the
 * constructor's JVM signature and fails at runtime with NoSuchMethodError (see SDK-172).
 * Create it with [SessionReplayConfigFactory] and set options through property setters.
 *
 * Callers must catch [LinkageError] around any use of this class, which is thrown when the
 * SDK is absent or incompatible.
 */
internal class MixpanelSessionReplayBridge(
    private val instanceProvider: () -> MPSessionReplayInstance? = { MPSessionReplay.getInstance() }
) : SessionReplayBridge {

    override val instance: Any? get() = instanceProvider()

    override fun identify(distinctId: String) {
        instanceProvider()?.identify(distinctId)
    }

    override fun isRecording(): Boolean = instanceProvider()?.isRecording() ?: false

    override fun startRecording(sessionsPercent: Double) {
        instanceProvider()?.startRecording(sessionsPercent)
    }

    override fun stopRecording() {
        instanceProvider()?.stopRecording()
    }

    override fun getReplayId(): String? = instanceProvider()?.getReplayId()

    companion object {
        fun initialize(
            context: Context,
            token: String,
            distinctId: String,
            configuration: SessionReplayConfiguration
        ): MixpanelSessionReplayBridge {
            MPSessionReplay.initialize(context, token, distinctId, buildConfig(configuration))
            return MixpanelSessionReplayBridge()
        }

        /** Options the kit does not set keep the SDK's defaults. */
        fun buildConfig(configuration: SessionReplayConfiguration): MPSessionReplayConfig =
            SessionReplayConfigFactory.create().apply {
                wifiOnly = configuration.wifiOnly
                flushInterval = configuration.flushIntervalSeconds.toLong()
                autoStartRecording = configuration.autoStartRecording
                recordingSessionsPercent = configuration.recordSessionsPercent
                autoMaskedViews = buildAutoMaskedViews(configuration)
                enableLogging = configuration.enableLogging
            }

        private fun buildAutoMaskedViews(configuration: SessionReplayConfiguration): Set<AutoMaskedView> {
            val result = mutableSetOf<AutoMaskedView>()
            if (configuration.maskText) result.add(AutoMaskedView.Text)
            if (configuration.maskImages) result.add(AutoMaskedView.Image)
            if (configuration.maskWebViews) result.add(AutoMaskedView.Web)
            return result
        }
    }
}
