package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mparticle.MPEvent
import com.mparticle.commerce.CommerceEvent
import org.json.JSONObject

/**
 * Test helper that exposes protected methods and allows mock injection for testing.
 * Overrides methods that create ReportingMessages to avoid null configuration issues in tests.
 */
class TestableMixpanelKit : MixpanelKit() {

    private var mockMixpanelAPI: MixpanelAPI? = null

    fun setMockMixpanelAPI(mock: MixpanelAPI) {
        mockMixpanelAPI = mock
    }

    public override fun onKitCreate(
        settings: Map<String, String>?,
        context: Context?
    ): List<ReportingMessage> {
        requireNotNull(context) { "Context is required" }
        val token = settings?.get(KEY_TOKEN)
        if (token.isNullOrEmpty()) {
            throw IllegalArgumentException("Mixpanel token is required")
        }

        // Parse configuration settings
        settings[KEY_USER_ID_TYPE]?.let { value ->
            UserIdentificationType.fromValue(value)?.let { setUserIdentificationType(it) }
        }
        settings[KEY_USE_PEOPLE]?.let { value ->
            setUseMixpanelPeople(value.lowercase() == "true")
        }

        // Parse Session Replay configuration
        setSessionReplayConfig(SessionReplayConfiguration.fromSettings(settings))

        // Use mock if available, otherwise call real SDK
        if (mockMixpanelAPI != null) {
            setMixpanelInstance(mockMixpanelAPI!!)
            setStarted(true)
            return emptyList()
        }

        return super.onKitCreate(settings, context)
    }

    // Override methods that create ReportingMessages to avoid null configuration issues
    override fun logEvent(event: MPEvent): List<ReportingMessage>? {
        if (!isStarted) return null
        val mixpanel = instance as? MixpanelAPI ?: return null
        val eventName = event.eventName
        if (eventName.isNullOrEmpty()) return null

        // Build properties with event type (matching new implementation)
        val properties = JSONObject()
        properties.put(EVENT_TYPE_PROPERTY, event.eventType.toString())
        event.customAttributeStrings?.forEach { (key, value) ->
            properties.put(key, value)
        }

        mixpanel.track(eventName, properties)
        return emptyList() // Return empty list to avoid ReportingMessage creation issues in tests
    }

    override fun logScreen(
        screenName: String?,
        screenAttributes: MutableMap<String, String>?
    ): List<ReportingMessage>? {
        if (!isStarted || screenName.isNullOrEmpty()) return null
        val mixpanel = instance as? MixpanelAPI ?: return null

        mixpanel.track("Viewed $screenName", convertToJSONObject(screenAttributes))
        return emptyList()
    }

    override fun logError(
        message: String?,
        errorAttributes: MutableMap<String, String>?
    ): List<ReportingMessage>? {
        if (!isStarted) return null
        val mixpanel = instance as? MixpanelAPI ?: return null

        val props = JSONObject().apply {
            put("error_message", message ?: "Unknown error")
            errorAttributes?.forEach { (key, value) -> put(key, value) }
        }
        mixpanel.track("Error", props)
        return emptyList()
    }

    override fun logException(
        exception: Exception?,
        exceptionAttributes: MutableMap<String, String>?,
        message: String?
    ): List<ReportingMessage>? {
        if (!isStarted) return null
        val mixpanel = instance as? MixpanelAPI ?: return null

        val props = JSONObject().apply {
            put("exception_message", message ?: exception?.message ?: "Unknown exception")
            put("exception_class", exception?.javaClass?.name ?: "Unknown")
            exceptionAttributes?.forEach { (key, value) -> put(key, value) }
        }
        mixpanel.track("Exception", props)
        return emptyList()
    }

    override fun leaveBreadcrumb(breadcrumb: String?): List<ReportingMessage>? {
        if (!isStarted || breadcrumb.isNullOrEmpty()) return null
        val mixpanel = instance as? MixpanelAPI ?: return null

        val props = JSONObject().apply {
            put("breadcrumb", breadcrumb)
        }
        mixpanel.track("Breadcrumb", props)
        return emptyList()
    }

    override fun logEvent(event: CommerceEvent): List<ReportingMessage>? {
        if (!isStarted) return null
        val mixpanel = instance as? MixpanelAPI ?: return null

        // Expand all commerce events (including purchases) to regular events
        // Note: trackCharge is deprecated by Mixpanel
        val expandedEvents = CommerceEventUtils.expand(event)

        expandedEvents?.forEach { expandedEvent ->
            val eventName = expandedEvent.eventName
            if (!eventName.isNullOrEmpty()) {
                val properties = buildCommerceEventProperties(expandedEvent, event)
                mixpanel.track(eventName, properties)
            }
        }

        return emptyList()
    }

    private fun convertToJSONObject(attributes: Map<String, String>?): JSONObject? {
        if (attributes.isNullOrEmpty()) return null
        return JSONObject().apply {
            attributes.forEach { (key, value) -> put(key, value) }
        }
    }
}
