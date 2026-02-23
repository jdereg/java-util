package com.cedarsoftware.test.external;

import java.util.concurrent.atomic.AtomicReference;

import com.cedarsoftware.util.ReflectionUtils;

/**
 * External-package caller utility used to verify ReflectionUtils trusted-caller controls.
 */
public final class ReflectionExternalCaller {

    private ReflectionExternalCaller() {
    }

    public static Throwable invokeArgCountLookupOnDangerousClass() {
        return runIsolated(new Runnable() {
            @Override
            public void run() {
                ReflectionUtils.getMethod(Runtime.getRuntime(), "exec", 1);
            }
        });
    }

    public static Throwable invokeCallByNameOnDangerousClass() {
        return runIsolated(new Runnable() {
            @Override
            public void run() {
                ReflectionUtils.call(Runtime.getRuntime(), "getRuntime");
            }
        });
    }

    public static Throwable invokeNonOverloadedLookupOnDangerousClass() {
        return runIsolated(new Runnable() {
            @Override
            public void run() {
                ReflectionUtils.getNonOverloadedMethod(Runtime.class, "getRuntime");
            }
        });
    }

    private static Throwable runIsolated(final Runnable action) {
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } catch (Throwable t) {
                    failure.set(t);
                }
            }
        }, "reflection-external-caller");

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return e;
        }
        return failure.get();
    }
}
