package com.kodedu.terminalfx;

import javafx.scene.media.AudioClip;

import java.util.concurrent.CompletableFuture;

/** Loads and plays the small bundled bell away from the application thread. */
final class Bell {
    private static final CompletableFuture<AudioClip> CLIP = CompletableFuture.supplyAsync(
            () -> new AudioClip(Bell.class.getResource("bell.wav").toExternalForm()));

    private Bell() {
    }

    static void play() {
        CLIP.thenAcceptAsync(AudioClip::play);
    }
}
