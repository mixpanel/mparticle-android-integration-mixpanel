package com.mparticle.kits

import android.content.Context
import android.util.Log
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
        // Validate required parameters - let exceptions propagate for validation errors
        requireNotNull(context) { "Context is required" }
        val token = settings?.get(KEY_TOKEN)
        if (token.isNullOrEmpty()) {
            throw IllegalArgumentException("Mixpanel token is required")
        }

        try {
            Log.d(LOG_TAG, "onKitCreate()")

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
            return listOf(
                ReportingMessage(
                    this,
                    ReportingMessage.MessageType.APP_STATE_TRANSITION,
                    System.currentTimeMillis(),
                    null
                )
            )
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onKitCreate(): ${t.message}", t)
            return emptyList()
        }
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "setOptOut(): Kit not started")
                return emptyList()
            }
            val mixpanel = mixpanelInstance ?: return emptyList()
            Log.d(LOG_TAG, "setOptOut(): optedOut=$optedOut")

            if (optedOut) {
                mixpanel.optOutTracking()
            } else {
                mixpanel.optInTracking()
            }
            return listOf(
                ReportingMessage(
                    this,
                    ReportingMessage.MessageType.OPT_OUT,
                    System.currentTimeMillis(),
                    null
                )
            )
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "setOptOut(): ${t.message}", t)
        }
        return emptyList()
    }

    override fun getInstance(): Any? = mixpanelInstance

    // EventListener implementation

    override fun logEvent(event: MPEvent): List<ReportingMessage>? {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "logEvent(MPEvent): Kit not started")
                return null
            }
            val mixpanel = mixpanelInstance ?: return null
            val eventName = event.eventName
            if (eventName.isNullOrEmpty()) return null

            Log.d(LOG_TAG, "logEvent(MPEvent): $eventName")

            // Build properties with event type (MoEngage pattern)
            val properties = JSONObject()
            properties.put(EVENT_TYPE_PROPERTY, event.eventType.toString())
            event.customAttributeStrings?.forEach { (key, value) ->
                properties.put(key, value)
            }

            mixpanel.track(eventName, properties)
            return listOf(ReportingMessage.fromEvent(this, event))
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "logEvent(MPEvent): ${t.message}", t)
        }
        return emptyList()
    }

    override fun logScreen(
        screenName: String?,
        screenAttributes: MutableMap<String, String>?
    ): List<ReportingMessage>? {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "logScreen(): Kit not started")
                return null
            }
            if (screenName.isNullOrEmpty()) return null
            val mixpanel = mixpanelInstance ?: return null

            Log.d(LOG_TAG, "logScreen(): $screenName")
            mixpanel.track("Viewed $screenName", convertToJSONObject(screenAttributes))
            return listOf(
                ReportingMessage(
                    this,
                    ReportingMessage.MessageType.SCREEN_VIEW,
                    System.currentTimeMillis(),
                    screenAttributes
                )
            )
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "logScreen(): ${t.message}", t)
        }
        return emptyList()
    }

    override fun logError(
        message: String?,
        errorAttributes: MutableMap<String, String>?
    ): List<ReportingMessage>? {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "logError(): Kit not started")
                return null
            }
            val mixpanel = mixpanelInstance ?: return null

            Log.d(LOG_TAG, "logError(): $message")
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
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "logError(): ${t.message}", t)
        }
        return emptyList()
    }

    override fun logException(
        exception: Exception?,
        exceptionAttributes: MutableMap<String, String>?,
        message: String?
    ): List<ReportingMessage>? {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "logException(): Kit not started")
                return null
            }
            val mixpanel = mixpanelInstance ?: return null

            Log.d(LOG_TAG, "logException(): ${exception?.javaClass?.name}")
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
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "logException(): ${t.message}", t)
        }
        return emptyList()
    }

    override fun leaveBreadcrumb(breadcrumb: String?): List<ReportingMessage>? {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "leaveBreadcrumb(): Kit not started")
                return null
            }
            if (breadcrumb.isNullOrEmpty()) return null
            val mixpanel = mixpanelInstance ?: return null

            Log.d(LOG_TAG, "leaveBreadcrumb(): $breadcrumb")
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
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "leaveBreadcrumb(): ${t.message}", t)
        }
        return emptyList()
    }

    // CommerceListener implementation

    override fun logEvent(event: CommerceEvent): List<ReportingMessage>? {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "logEvent(CommerceEvent): Kit not started")
                return null
            }
            val mixpanel = mixpanelInstance ?: return null

            Log.d(LOG_TAG, "logEvent(CommerceEvent): ${event.productAction}")

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
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "logEvent(CommerceEvent): ${t.message}", t)
        }
        return emptyList()
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
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onIdentifyCompleted(): Kit not started")
                return
            }
            Log.d(LOG_TAG, "onIdentifyCompleted()")
            identifyUser(user)
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onIdentifyCompleted(): ${t.message}", t)
        }
    }

    override fun onLoginCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onLoginCompleted(): Kit not started")
                return
            }
            Log.d(LOG_TAG, "onLoginCompleted()")
            identifyUser(user)
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onLoginCompleted(): ${t.message}", t)
        }
    }

    override fun onLogoutCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onLogoutCompleted(): Kit not started")
                return
            }
            Log.d(LOG_TAG, "onLogoutCompleted()")
            mixpanelInstance?.reset()
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onLogoutCompleted(): ${t.message}", t)
        }
    }

    override fun onModifyCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onModifyCompleted(): Kit not started")
                return
            }
            Log.d(LOG_TAG, "onModifyCompleted()")
            identifyUser(user)
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onModifyCompleted(): ${t.message}", t)
        }
    }

    override fun onUserIdentified(user: MParticleUser?) {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onUserIdentified(): Kit not started")
                return
            }
            Log.d(LOG_TAG, "onUserIdentified()")
            identifyUser(user)
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onUserIdentified(): ${t.message}", t)
        }
    }

    private fun identifyUser(user: MParticleUser?) {
        try {
            val userId = extractUserId(user) ?: return
            Log.d(LOG_TAG, "identifyUser(): $userId")
            mixpanelInstance?.identify(userId)
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "identifyUser(): ${t.message}", t)
        }
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
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onSetUserAttribute(): Kit not started")
                return
            }
            if (key.isNullOrEmpty() || value == null) return
            val mixpanel = mixpanelInstance ?: return

            Log.d(LOG_TAG, "onSetUserAttribute(): $key")
            val mappedKey = mapAttributeKey(key)
            if (useMixpanelPeople) {
                mixpanel.people.set(mappedKey, value)
            } else {
                mixpanel.registerSuperProperties(JSONObject().apply { put(mappedKey, value) })
            }
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onSetUserAttribute(): ${t.message}", t)
        }
    }

    override fun onRemoveUserAttribute(key: String?, user: FilteredMParticleUser?) {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onRemoveUserAttribute(): Kit not started")
                return
            }
            if (key.isNullOrEmpty()) return
            val mixpanel = mixpanelInstance ?: return

            Log.d(LOG_TAG, "onRemoveUserAttribute(): $key")
            val mappedKey = mapAttributeKey(key)
            if (useMixpanelPeople) {
                mixpanel.people.unset(mappedKey)
            } else {
                mixpanel.unregisterSuperProperty(mappedKey)
            }
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onRemoveUserAttribute(): ${t.message}", t)
        }
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?
    ) {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onIncrementUserAttribute(): Kit not started")
                return
            }
            if (key.isNullOrEmpty()) return

            Log.d(LOG_TAG, "onIncrementUserAttribute(): $key by $incrementedBy")
            val mappedKey = mapAttributeKey(key)
            if (useMixpanelPeople) {
                mixpanelInstance?.people?.increment(mappedKey, incrementedBy?.toDouble() ?: 0.0)
            }
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onIncrementUserAttribute(): ${t.message}", t)
        }
    }

    override fun onSetUserTag(key: String?, user: FilteredMParticleUser?) {
        try {
            Log.d(LOG_TAG, "onSetUserTag(): $key")
            onSetUserAttribute(key, true, user)
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onSetUserTag(): ${t.message}", t)
        }
    }

    override fun onSetUserAttributeList(
        key: String?,
        values: MutableList<String>?,
        user: FilteredMParticleUser?
    ) {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onSetUserAttributeList(): Kit not started")
                return
            }
            if (key.isNullOrEmpty() || values.isNullOrEmpty()) return
            val mixpanel = mixpanelInstance ?: return

            Log.d(LOG_TAG, "onSetUserAttributeList(): $key")
            val mappedKey = mapAttributeKey(key)
            val jsonArray = JSONArray(values)
            if (useMixpanelPeople) {
                mixpanel.people.set(mappedKey, jsonArray)
            } else {
                mixpanel.registerSuperProperties(JSONObject().apply { put(mappedKey, jsonArray) })
            }
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onSetUserAttributeList(): ${t.message}", t)
        }
    }

    override fun onSetAllUserAttributes(
        attributes: MutableMap<String, String>?,
        attributeLists: MutableMap<String, MutableList<String>>?,
        user: FilteredMParticleUser?
    ) {
        try {
            if (!_isStarted) {
                Log.w(LOG_TAG, "onSetAllUserAttributes(): Kit not started")
                return
            }
            Log.d(LOG_TAG, "onSetAllUserAttributes()")
            attributes?.forEach { (key, value) -> onSetUserAttribute(key, value, user) }
            attributeLists?.forEach { (key, values) -> onSetUserAttributeList(key, values, user) }
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "onSetAllUserAttributes(): ${t.message}", t)
        }
    }

    override fun supportsAttributeLists(): Boolean = true

    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        user: FilteredMParticleUser?
    ) {
        // No-op: Mixpanel doesn't have a consent API that maps to mParticle's
        Log.d(LOG_TAG, "onConsentStateUpdated(): No-op")
    }

    // Helper methods

    private fun convertToJSONObject(attributes: Map<String, String>?): JSONObject? {
        if (attributes.isNullOrEmpty()) return null
        return JSONObject().apply {
            attributes.forEach { (key, value) -> put(key, value) }
        }
    }

    /**
     * Maps an mParticle user attribute key to the corresponding Mixpanel reserved property key.
     * If no mapping exists, returns the original key unchanged.
     */
    private fun mapAttributeKey(mParticleKey: String): String {
        return RESERVED_ATTRIBUTE_MAP[mParticleKey] ?: mParticleKey
    }

    /**
     * Maps mParticle user attribute keys to Mixpanel reserved profile property keys.
     * See: https://docs.mixpanel.com/docs/data-structure/property-reference/reserved-properties
     */
    private companion object {
        private val RESERVED_ATTRIBUTE_MAP: Map<String, String> = mapOf(
            MParticle.UserAttributes.FIRSTNAME to "\$first_name",
            MParticle.UserAttributes.LASTNAME to "\$last_name",
            MParticle.UserAttributes.MOBILE_NUMBER to "\$phone",
            MParticle.UserAttributes.CITY to "\$city",
            MParticle.UserAttributes.STATE to "\$region",
            MParticle.UserAttributes.COUNTRY to "\$country_code",
        )
    }

}
