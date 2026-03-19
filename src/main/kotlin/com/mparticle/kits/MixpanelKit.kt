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

    // Session Replay
    @Volatile
    private var sessionReplayConfig: SessionReplayConfiguration = SessionReplayConfiguration()

    @Volatile
    private var _sessionReplayInstance: Any? = null

    @Volatile
    private var _sessionReplayClass: Class<*>? = null

    @Volatile
    private var wasManuallyStoppedBeforeOptOut: Boolean = false

    private val sessionReplayInstanceLock = Any()

    /** Access to the underlying Session Replay SDK instance, if available. */
    val sessionReplayInstance: Any? get() = resolveSessionReplayInstance()

    /** Whether Session Replay is enabled and initialized. */
    val isSessionReplayEnabled: Boolean get() = sessionReplayConfig.enabled && resolveSessionReplayInstance() != null

    private fun resolveSessionReplayInstance(): Any? {
        _sessionReplayInstance?.let { return it }
        val srClass = _sessionReplayClass ?: return null
        synchronized(sessionReplayInstanceLock) {
            // Double-check after acquiring lock (another thread may have resolved)
            _sessionReplayInstance?.let { return it }
            return try {
                val companionField = srClass.getField("Companion")
                val companion = companionField.get(null)
                val getInstanceMethod = companion.javaClass.getMethod("getInstance")
                val instance = getInstanceMethod.invoke(companion)
                if (instance != null) {
                    _sessionReplayInstance = instance
                    Log.d(LOG_TAG, "Session Replay instance resolved (async init complete)")
                } else {
                    Log.d(LOG_TAG, "Session Replay instance not yet available (getInstance() returned null)")
                }
                instance
            } catch (e: NoSuchFieldException) {
                Log.w(
                    LOG_TAG,
                    "Session Replay integration error: Companion field not found on ${srClass.name}. Disabling Session Replay.",
                    e
                )
                _sessionReplayClass = null
                null
            } catch (e: NoSuchMethodException) {
                Log.w(
                    LOG_TAG,
                    "Session Replay integration error: getInstance() method not found on ${srClass.name}. Disabling Session Replay.",
                    e
                )
                _sessionReplayClass = null
                null
            } catch (e: Exception) {
                Log.e(
                    LOG_TAG,
                    "Session Replay integration error while resolving instance. Disabling Session Replay.",
                    e
                )
                _sessionReplayClass = null
                null
            }
        }
    }

    val isStarted: Boolean get() = _isStarted

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

    protected fun setSessionReplayConfig(config: SessionReplayConfiguration) {
        sessionReplayConfig = config
    }

    protected fun setSessionReplayInstance(instance: Any?) {
        _sessionReplayInstance = instance
    }

    protected fun setSessionReplayClass(clazz: Class<*>?) {
        _sessionReplayClass = clazz
    }

    protected fun setWasManuallyStoppedBeforeOptOut(value: Boolean) {
        wasManuallyStoppedBeforeOptOut = value
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
            @Suppress("DEPRECATION")
            val baseUrl = (settings[KEY_BASE_URL] ?: settings[KEY_SERVER_URL])?.takeIf { it.isNotEmpty() }
            settings[KEY_USER_ID_TYPE]?.let { value ->
                UserIdentificationType.fromValue(value)?.let { userIdentificationType = it }
            }
            settings[KEY_USE_PEOPLE]?.let { value ->
                useMixpanelPeople = value.lowercase() == "true"
            }

            // Parse Session Replay configuration
            sessionReplayConfig = SessionReplayConfiguration.fromSettings(settings)

            // Initialize Mixpanel SDK
            mixpanelInstance = MixpanelAPI.getInstance(context, token, false)
            baseUrl?.let { mixpanelInstance?.setServerURL(it) }

            // onKitCreate() is called both on first init and on opt-in (mParticle recreates the kit).
            // Mixpanel persists opt-out state, so clear it if stale from a previous opt-out cycle.
            if (mixpanelInstance?.hasOptedOutTracking() == true) {
                mixpanelInstance?.optInTracking()
                Log.d(LOG_TAG, "onKitCreate(): cleared stale Mixpanel opt-out state")
            }

            _isStarted = true

            // Initialize Session Replay if enabled
            initializeSessionReplayIfEnabled(context, token)

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
                // Check if recording was manually stopped before opt-out
                if (resolveSessionReplayInstance() != null && !isSessionReplayRecording()) {
                    wasManuallyStoppedBeforeOptOut = true
                }
                mixpanel.optOutTracking()
                stopSessionReplayRecording()
            } else {
                mixpanel.optInTracking()
                if (sessionReplayConfig.enabled && sessionReplayConfig.autoStartRecording
                    && !wasManuallyStoppedBeforeOptOut
                ) {
                    // Sync identity before restarting — distinctId may have changed while opted out
                    val currentDistinctId = mixpanel.distinctId
                    if (!currentDistinctId.isNullOrEmpty()) {
                        identifySessionReplay(currentDistinctId)
                    }
                    startSessionReplayRecording()
                }
                wasManuallyStoppedBeforeOptOut = false
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
            // Restart recording for the new logged-in user if auto-start is enabled
            if (sessionReplayConfig.enabled && sessionReplayConfig.autoStartRecording) {
                startSessionReplayRecording()
            }
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
            stopSessionReplayRecording()
            wasManuallyStoppedBeforeOptOut = false
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
            identifySessionReplay(userId)
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
    
    // Session Replay methods

    /**
     * Initialize Session Replay if enabled and the SDK is available.
     * Uses reflection to avoid ClassNotFoundException when the optional dependency is not present.
     */
    private fun initializeSessionReplayIfEnabled(context: Context, token: String) {
        if (!sessionReplayConfig.enabled) {
            Log.d(LOG_TAG, "Session Replay is disabled")
            return
        }

        try {
            // Check if Session Replay SDK is available
            val sessionReplayClass = Class.forName("com.mixpanel.android.sessionreplay.MPSessionReplay")
            val configClass = Class.forName("com.mixpanel.android.sessionreplay.models.MPSessionReplayConfig")
            val autoMaskedViewClass = Class.forName("com.mixpanel.android.sessionreplay.sensitive_views.AutoMaskedView")

            // Build autoMaskedViews set based on mask configuration
            val autoMaskedViews = buildAutoMaskedViewsSet(autoMaskedViewClass)

            // Create MPSessionReplayConfig using data class constructor
            // Constructor params: wifiOnly, flushInterval, autoStartRecording, recordingSessionsPercent, autoMaskedViews, enableLogging
            val configConstructor = configClass.getConstructor(
                Boolean::class.java,      // wifiOnly
                Long::class.java,         // flushInterval
                Boolean::class.java,      // autoStartRecording
                Double::class.java,       // recordingSessionsPercent
                Set::class.java,          // autoMaskedViews
                Boolean::class.java       // enableLogging
            )

            val config = configConstructor.newInstance(
                sessionReplayConfig.wifiOnly,
                sessionReplayConfig.flushIntervalSeconds.toLong(),
                sessionReplayConfig.autoStartRecording,
                sessionReplayConfig.recordSessionsPercent,
                autoMaskedViews,
                sessionReplayConfig.enableLogging
            )

            // Get the distinct ID from Mixpanel
            val distinctId = mixpanelInstance?.distinctId ?: ""

            // Initialize Session Replay
            val initializeMethod = sessionReplayClass.getMethod(
                "initialize",
                Context::class.java,
                String::class.java,
                String::class.java,
                configClass
            )

            initializeMethod.invoke(null, context.applicationContext, token, distinctId, config)
            _sessionReplayClass = sessionReplayClass
            resolveSessionReplayInstance()

            val instanceResolved = _sessionReplayInstance != null
            val integrationDisabled = _sessionReplayClass == null && !instanceResolved
            when {
                instanceResolved -> {
                    Log.i(LOG_TAG, "Session Replay initialized, instance resolved synchronously")
                }
                integrationDisabled -> {
                    Log.w(LOG_TAG, "Session Replay initialization attempted, but integration was disabled due to reflection/API mismatch; instance will not be resolved lazily")
                }
                else -> {
                    Log.i(LOG_TAG, "Session Replay initialized, instance not yet available (will resolve lazily if Session Replay completes async init)")
                }
            }

        } catch (e: ClassNotFoundException) {
            Log.w(LOG_TAG, "Session Replay SDK not available. Add 'com.mixpanel.android:mixpanel-android-session-replay' dependency to enable.", e)
        } catch (e: NoSuchMethodException) {
            Log.e(LOG_TAG, "Session Replay SDK API mismatch: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to initialize Session Replay: ${e.message}", e)
            _sessionReplayClass = null
            _sessionReplayInstance = null
        }
    }

    /**
     * Build autoMaskedViews set from boolean mask flags.
     * Maps maskImages/maskText/maskWebViews to AutoMaskedView enum values.
     */
    private fun buildAutoMaskedViewsSet(autoMaskedViewClass: Class<*>): Set<Any> {
        val result = mutableSetOf<Any>()

        // Get enum constants: Text, Image, Web
        val enumConstants = autoMaskedViewClass.enumConstants as? Array<*> ?: return emptySet()

        for (constant in enumConstants) {
            val name = (constant as Enum<*>).name
            val shouldInclude = when (name) {
                "Text" -> sessionReplayConfig.maskText
                "Image" -> sessionReplayConfig.maskImages
                "Web" -> sessionReplayConfig.maskWebViews
                else -> false
            }
            if (shouldInclude) {
                result.add(constant)
            }
        }

        return result
    }

    /**
     * Sync identity with Session Replay.
     */
    private fun identifySessionReplay(distinctId: String) {
        val instance = resolveSessionReplayInstance() ?: return

        try {
            val sessionReplayClass = instance.javaClass
            val identifyMethod = sessionReplayClass.getMethod("identify", String::class.java)
            identifyMethod.invoke(instance, distinctId)
            Log.d(LOG_TAG, "Session Replay identity synced")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to sync Session Replay identity: ${e.message}", e)
        }
    }

    /**
     * Check if Session Replay is currently recording.
     * Uses reflection to avoid compile-time dependency.
     */
    private fun isSessionReplayRecording(): Boolean {
        val instance = resolveSessionReplayInstance() ?: return false
        return try {
            val method = instance.javaClass.getMethod("isRecording")
            method.invoke(instance) as? Boolean ?: false
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to check Session Replay recording state: ${e.message}", e)
            false
        }
    }

    /**
     * Start Session Replay recording.
     */
    fun startSessionReplayRecording() {
        if (!_isStarted) return
        val instance = resolveSessionReplayInstance() ?: return

        try {
            val sessionReplayClass = instance.javaClass
            val startRecordingMethod = sessionReplayClass.getMethod("startRecording", Double::class.javaPrimitiveType)
            startRecordingMethod.invoke(instance, sessionReplayConfig.recordSessionsPercent)
            Log.d(LOG_TAG, "Session Replay recording started")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to start Session Replay recording: ${e.message}", e)
        }
    }

    /**
     * Stop Session Replay recording.
     */
    fun stopSessionReplayRecording() {
        if (!_isStarted) return
        val instance = resolveSessionReplayInstance() ?: return

        try {
            val sessionReplayClass = instance.javaClass
            val stopRecordingMethod = sessionReplayClass.getMethod("stopRecording")
            stopRecordingMethod.invoke(instance)
            Log.d(LOG_TAG, "Session Replay recording stopped")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to stop Session Replay recording: ${e.message}", e)
        }
    }

    /**
     * Get the current Session Replay ID, if available.
     *
     * @return The session replay ID or null if not available
     */
    fun getSessionReplayId(): String? {
        if (!_isStarted) return null
        val instance = resolveSessionReplayInstance() ?: return null

        return try {
            val sessionReplayClass = instance.javaClass
            val getReplayIdMethod = sessionReplayClass.getMethod("getReplayId")
            getReplayIdMethod.invoke(instance) as? String
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to get Session Replay ID: ${e.message}", e)
            null
        }
    }
}
