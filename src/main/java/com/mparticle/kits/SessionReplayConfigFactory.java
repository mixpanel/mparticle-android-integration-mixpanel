package com.mparticle.kits;

import com.mixpanel.android.sessionreplay.models.MPSessionReplayConfig;

/**
 * Creates {@link MPSessionReplayConfig} through its true no-arg constructor.
 *
 * <p>This is Java on purpose. In Kotlin, {@code MPSessionReplayConfig()} compiles to the
 * synthetic default-arguments constructor, whose JVM signature lists every constructor
 * parameter and changes whenever the Session Replay SDK adds an option (see SDK-172). The
 * no-arg constructor Kotlin generates for Java callers has a signature that never changes.
 */
final class SessionReplayConfigFactory {
    private SessionReplayConfigFactory() {}

    static MPSessionReplayConfig create() {
        return new MPSessionReplayConfig();
    }
}
