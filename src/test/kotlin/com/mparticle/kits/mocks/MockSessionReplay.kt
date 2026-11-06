package com.mparticle.kits.mocks

/**
 * Minimal stub providing the Companion.getInstance() structure that
 * resolveSessionReplayInstance() resolves via reflection. Methods are open
 * so Mockito can mock the class and reflection can still find them on the proxy.
 */
open class MockSessionReplay {

    open fun identify(distinctId: String) {}
    open fun startRecording(percent: Double) {}
    open fun stopRecording() {}
    open fun isRecording(): Boolean = false
    open fun getReplayId(): String? = null

    companion object {
        @Volatile
        private var instance: MockSessionReplay? = null

        fun getInstance(): MockSessionReplay? = instance

        fun setInstance(mock: MockSessionReplay?) {
            instance = mock
        }

        fun reset() {
            instance = null
        }
    }
}

/**
 * Stub with a Companion that lacks getInstance().
 * Triggers NoSuchMethodException in resolveSessionReplayInstance().
 */
class MockSessionReplayNoGetInstance {
    companion object
}
