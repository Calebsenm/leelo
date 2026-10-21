package com.leelo;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Starts the JavaFX toolkit once for every test of the run.
 *
 * <p>{@link Platform#startup(Runnable)} can only be called once per JVM, so the
 * tests share this helper instead of starting the toolkit by themselves.
 */
final class JavaFxTestSupport {

    private static boolean started;

    private JavaFxTestSupport() {
    }

    static synchronized void startToolkit() throws InterruptedException {
        if (started) {
            return;
        }
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(30, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            // Another test of the run started the toolkit first; it is shared.
        }
        started = true;
    }
}
