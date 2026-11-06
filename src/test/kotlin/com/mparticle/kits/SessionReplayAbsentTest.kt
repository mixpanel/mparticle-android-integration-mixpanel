package com.mparticle.kits

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.Callable

/**
 * Runs the kit with the Session Replay SDK removed from its classpath, the way it runs in an
 * app that doesn't depend on Session Replay.
 *
 * The SDK is on the unit test classpath, so the kit's classes are loaded through
 * [SessionReplayHidingClassLoader], which makes every Session Replay class unresolvable. This
 * exercises the real bridge creation path rather than a simulated failure.
 */
class SessionReplayAbsentTest {

    private val loader = SessionReplayHidingClassLoader(javaClass.classLoader!!)

    @Test
    fun `session replay classes are hidden from the kit`() {
        assertThrows(ClassNotFoundException::class.java) {
            Class.forName("com.mixpanel.android.sessionreplay.MPSessionReplay", false, loader)
        }
    }

    @Test
    fun `kit classes are loaded through the hiding loader`() {
        assertSame(loader, loader.loadClass(MixpanelKit::class.java.name).classLoader)
        assertSame(loader, loader.loadClass(MixpanelSessionReplayBridge::class.java.name).classLoader)
    }

    @Test
    fun `kit loads and runs with session replay enabled but SDK absent`() {
        @Suppress("UNCHECKED_CAST")
        val scenario = loader.loadClass(SessionReplayAbsentScenario::class.java.name)
            .getDeclaredConstructor()
            .newInstance() as Callable<List<Any?>>

        val (instance, enabled, replayId) = scenario.call()

        assertEquals(null, instance)
        assertEquals(false, enabled)
        assertEquals(null, replayId)
    }
}

/** Loaded through [SessionReplayHidingClassLoader]; see [SessionReplayAbsentTest]. */
class SessionReplayAbsentScenario : Callable<List<Any?>> {
    override fun call(): List<Any?> {
        val kit = object : MixpanelKit() {
            fun enableSessionReplay() {
                setSessionReplayConfig(SessionReplayConfiguration(enabled = true))
                setStarted(true)
            }
        }
        kit.enableSessionReplay()
        kit.initializeSessionReplayIfEnabled(mock(Context::class.java), "test-token")
        kit.startSessionReplayRecording()
        kit.stopSessionReplayRecording()
        return listOf(kit.sessionReplayInstance, kit.isSessionReplayEnabled, kit.getSessionReplayId())
    }
}

/**
 * Hides the Session Replay SDK and loads this project's own classes itself (rather than
 * delegating to [parent]), so their references to the SDK resolve through this loader.
 * Everything else, including the mParticle and Mixpanel SDKs, comes from [parent].
 */
private class SessionReplayHidingClassLoader(parent: ClassLoader) : ClassLoader(parent) {

    // Unit tests see main classes as a jar and test classes as a directory
    private val mainClassesJar = parent.getResource("com/mparticle/kits/MixpanelKit.class")!!
        .toString().substringBefore("!/")

    private fun isProjectClass(resource: java.net.URL): Boolean =
        resource.protocol == "file" || resource.toString().startsWith("$mainClassesJar!/")

    override fun loadClass(name: String, resolve: Boolean): Class<*> =
        synchronized(this) {
            findLoadedClass(name)?.let { return it }
            if (name.startsWith("com.mixpanel.android.sessionreplay.")) {
                throw ClassNotFoundException(name)
            }
            val resource = parent.getResource(name.replace('.', '/') + ".class")
            if (resource != null && isProjectClass(resource)) {
                val bytes = resource.readBytes()
                return defineClass(name, bytes, 0, bytes.size)
            }
            super.loadClass(name, resolve)
        }
}
