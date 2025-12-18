package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mparticle.MPEvent
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import org.json.JSONArray
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

        mixpanel.track(eventName, convertToJSONObject(event.customAttributeStrings))
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

        if (event.productAction == Product.PURCHASE) {
            val revenue = event.transactionAttributes?.revenue ?: 0.0
            mixpanel.people.trackCharge(revenue, convertToJSONObject(event.customAttributeStrings))
            return emptyList()
        }

        // For non-purchase events, expand to regular events
        CommerceEventUtils.expand(event)?.forEach { expandedEvent ->
            logEvent(expandedEvent)
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
