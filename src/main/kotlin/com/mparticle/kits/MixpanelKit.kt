package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

open class MixpanelKit : KitIntegration(),
    KitIntegration.EventListener,
    KitIntegration.CommerceListener,
    KitIntegration.IdentityListener,
    KitIntegration.UserAttributeListener {

    @Volatile
    private var mixpanelInstance: MixpanelAPI? = null

    @Volatile
    private var _isStarted: Boolean = false

    private var useMixpanelPeople: Boolean = true
    private var userIdentificationType: UserIdentificationType = UserIdentificationType.CUSTOMER_ID

    val isStarted: Boolean get() = _isStarted

    // Protected setters for testing
    protected fun setMixpanelInstance(instance: MixpanelAPI) {
        mixpanelInstance = instance
    }

    protected fun setStarted(started: Boolean) {
        _isStarted = started
    }

    protected fun setUseMixpanelPeople(usePeople: Boolean) {
        useMixpanelPeople = usePeople
    }

    protected fun setUserIdentificationType(type: UserIdentificationType) {
        userIdentificationType = type
    }

    override fun getName(): String = NAME

    override fun onKitCreate(
        settings: Map<String, String>?,
        context: Context?
    ): List<ReportingMessage> {
        requireNotNull(context) { "Context is required" }
        val token = settings?.get(KEY_TOKEN)
        if (token.isNullOrEmpty()) {
            throw IllegalArgumentException("Mixpanel token is required")
        }

        // Parse configuration settings
        val serverURL = settings[KEY_SERVER_URL]?.takeIf { it.isNotEmpty() }
        settings[KEY_USER_ID_TYPE]?.let { value ->
            UserIdentificationType.fromValue(value)?.let { userIdentificationType = it }
        }
        settings[KEY_USE_PEOPLE]?.let { value ->
            useMixpanelPeople = value.lowercase() == "true"
        }

        // Initialize Mixpanel SDK
        mixpanelInstance = MixpanelAPI.getInstance(context, token, false)
        serverURL?.let { mixpanelInstance?.setServerURL(it) }

        _isStarted = true
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
        if (!_isStarted) return emptyList()
        val mixpanel = mixpanelInstance ?: return emptyList()
        if (optedOut) {
            mixpanel.optOutTracking()
        } else {
            mixpanel.optInTracking()
        }
        return emptyList()
    }

    override fun getInstance(): Any? = mixpanelInstance

    // EventListener implementation

    override fun logEvent(event: MPEvent): List<ReportingMessage>? {
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null
        val eventName = event.eventName
        if (eventName.isNullOrEmpty()) return null

        mixpanel.track(eventName, convertToJSONObject(event.customAttributeStrings))
        return listOf(ReportingMessage.fromEvent(this, event))
    }

    override fun logScreen(
        screenName: String?,
        screenAttributes: MutableMap<String, String>?
    ): List<ReportingMessage>? {
        if (!_isStarted || screenName.isNullOrEmpty()) return null
        val mixpanel = mixpanelInstance ?: return null

        mixpanel.track("Viewed $screenName", convertToJSONObject(screenAttributes))
        return listOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.SCREEN_VIEW,
                System.currentTimeMillis(),
                screenAttributes
            )
        )
    }

    override fun logError(
        message: String?,
        errorAttributes: MutableMap<String, String>?
    ): List<ReportingMessage>? {
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null

        val props = JSONObject().apply {
            put("error_message", message ?: "Unknown error")
            errorAttributes?.forEach { (key, value) -> put(key, value) }
        }
        mixpanel.track("Error", props)
        return listOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.ERROR,
                System.currentTimeMillis(),
                errorAttributes
            )
        )
    }

    override fun logException(
        exception: Exception?,
        exceptionAttributes: MutableMap<String, String>?,
        message: String?
    ): List<ReportingMessage>? {
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null

        val props = JSONObject().apply {
            put("exception_message", message ?: exception?.message ?: "Unknown exception")
            put("exception_class", exception?.javaClass?.name ?: "Unknown")
            exceptionAttributes?.forEach { (key, value) -> put(key, value) }
        }
        mixpanel.track("Exception", props)
        return listOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.ERROR,
                System.currentTimeMillis(),
                exceptionAttributes
            )
        )
    }

    override fun leaveBreadcrumb(breadcrumb: String?): List<ReportingMessage>? {
        if (!_isStarted || breadcrumb.isNullOrEmpty()) return null
        val mixpanel = mixpanelInstance ?: return null

        val props = JSONObject().apply {
            put("breadcrumb", breadcrumb)
        }
        mixpanel.track("Breadcrumb", props)
        return listOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.BREADCRUMB,
                System.currentTimeMillis(),
                null
            )
        )
    }

    // CommerceListener implementation

    override fun logEvent(event: CommerceEvent): List<ReportingMessage>? {
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null

        // Expand all commerce events (including purchases) to regular events
        // Note: trackCharge is deprecated by Mixpanel - commerce events should be tracked as regular events
        val messages = mutableListOf<ReportingMessage>()
        val expandedEvents = CommerceEventUtils.expand(event)

        expandedEvents?.forEach { expandedEvent ->
            val eventName = expandedEvent.eventName
            if (!eventName.isNullOrEmpty()) {
                val properties = buildCommerceEventProperties(expandedEvent, event)
                mixpanel.track(eventName, properties)
                messages.add(ReportingMessage.fromEvent(this, expandedEvent))
            }
        }

        return messages.ifEmpty { null }
    }

    /**
     * Builds Mixpanel properties from expanded commerce event and original commerce event.
     * Combines: expanded event attributes, commerce event custom attributes, and transaction attributes.
     */
    internal fun buildCommerceEventProperties(
        expandedEvent: MPEvent,
        commerceEvent: CommerceEvent
    ): JSONObject {
        val properties = JSONObject()

        // Add expanded event's custom attributes (contains product info)
        expandedEvent.customAttributeStrings?.forEach { (key, value) ->
            properties.put(key, value)
        }

        // Add commerce event's custom attributes
        commerceEvent.customAttributeStrings?.forEach { (key, value) ->
            properties.put(key, value)
        }

        // Add transaction attributes
        commerceEvent.transactionAttributes?.let { txnAttrs ->
            txnAttrs.revenue?.let { properties.put("Revenue", it) }
            txnAttrs.id?.let { properties.put("Transaction Id", it) }
            txnAttrs.tax?.let { properties.put("Tax", it) }
            txnAttrs.shipping?.let { properties.put("Shipping", it) }
            txnAttrs.couponCode?.let { properties.put("Coupon Code", it) }
        }

        return properties
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal?,
        valueTotal: BigDecimal?,
        eventName: String?,
        contextInfo: MutableMap<String, String>?
    ): List<ReportingMessage>? {
        // Mixpanel doesn't have a direct LTV API, return null
        return null
    }

    // IdentityListener implementation

    override fun onIdentifyCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        if (_isStarted) identifyUser(user)
    }

    override fun onLoginCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        if (_isStarted) identifyUser(user)
    }

    override fun onLogoutCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        if (_isStarted) mixpanelInstance?.reset()
    }

    override fun onModifyCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        if (_isStarted) identifyUser(user)
    }

    override fun onUserIdentified(user: MParticleUser?) {
        if (_isStarted) identifyUser(user)
    }

    private fun identifyUser(user: MParticleUser?) {
        val userId = extractUserId(user) ?: return
        mixpanelInstance?.identify(userId)
    }

    private fun extractUserId(user: MParticleUser?): String? {
        val identities = user?.userIdentities ?: return null
        return when (userIdentificationType) {
            UserIdentificationType.CUSTOMER_ID -> identities[MParticle.IdentityType.CustomerId]
            UserIdentificationType.MPID -> user.id.toString()
            UserIdentificationType.OTHER -> identities[MParticle.IdentityType.Other]
            UserIdentificationType.OTHER_2 -> identities[MParticle.IdentityType.Other2]
            UserIdentificationType.OTHER_3 -> identities[MParticle.IdentityType.Other3]
            UserIdentificationType.OTHER_4 -> identities[MParticle.IdentityType.Other4]
        }
    }

    // UserAttributeListener implementation

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
        user: FilteredMParticleUser?
    ) {
        if (!_isStarted || key.isNullOrEmpty() || value == null) return
        val mixpanel = mixpanelInstance ?: return

        if (useMixpanelPeople) {
            mixpanel.people.set(key, value)
        } else {
            mixpanel.registerSuperProperties(JSONObject().apply { put(key, value) })
        }
    }

    override fun onRemoveUserAttribute(key: String?, user: FilteredMParticleUser?) {
        if (!_isStarted || key.isNullOrEmpty()) return
        val mixpanel = mixpanelInstance ?: return

        if (useMixpanelPeople) {
            mixpanel.people.unset(key)
        } else {
            mixpanel.unregisterSuperProperty(key)
        }
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?
    ) {
        if (!_isStarted || key.isNullOrEmpty()) return
        if (useMixpanelPeople) {
            mixpanelInstance?.people?.increment(key, incrementedBy?.toDouble() ?: 0.0)
        }
    }

    override fun onSetUserTag(key: String?, user: FilteredMParticleUser?) {
        onSetUserAttribute(key, true, user)
    }

    override fun onSetUserAttributeList(
        key: String?,
        values: MutableList<String>?,
        user: FilteredMParticleUser?
    ) {
        if (!_isStarted || key.isNullOrEmpty() || values.isNullOrEmpty()) return
        val mixpanel = mixpanelInstance ?: return

        val jsonArray = JSONArray(values)
        if (useMixpanelPeople) {
            mixpanel.people.set(key, jsonArray)
        } else {
            mixpanel.registerSuperProperties(JSONObject().apply { put(key, jsonArray) })
        }
    }

    override fun onSetAllUserAttributes(
        attributes: MutableMap<String, String>?,
        attributeLists: MutableMap<String, MutableList<String>>?,
        user: FilteredMParticleUser?
    ) {
        attributes?.forEach { (key, value) -> onSetUserAttribute(key, value, user) }
        attributeLists?.forEach { (key, values) -> onSetUserAttributeList(key, values, user) }
    }

    override fun supportsAttributeLists(): Boolean = true

    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        user: FilteredMParticleUser?
    ) {
        // No-op: Mixpanel doesn't have a consent API that maps to mParticle's
    }

    // Helper methods

    private fun convertToJSONObject(attributes: Map<String, String>?): JSONObject? {
        if (attributes.isNullOrEmpty()) return null
        return JSONObject().apply {
            attributes.forEach { (key, value) -> put(key, value) }
        }
    }

    companion object {
        const val NAME = "Mixpanel"
        const val KEY_TOKEN = "token"
        const val KEY_SERVER_URL = "serverURL"
        const val KEY_USER_ID_TYPE = "userIdentificationType"
        const val KEY_USE_PEOPLE = "useMixpanelPeople"
    }
}
