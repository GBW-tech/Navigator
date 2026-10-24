package com.nav.navigator.core;

import android.content.Context;
import android.util.Log;
import java.util.UUID;

public final class Protocol {
    public static final Protocol INSTANCE = new Protocol();
    private static final String TAG = "Protocol";
    private static boolean sLoaded = false;

    static {
        try {
            System.loadLibrary("kkprotocol");
            sLoaded = true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load native library kkprotocol", t);
        }
    }

    private native byte[] nativeA(byte[] frame);
    private native byte[] nativeB(int i, int i2, int i3, int i4);
    private native byte[] nativeC();
    private native byte[] nativeD();
    private native byte[] nativeE(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10);
    private native int nativeF(int i, int i2);
    private native String nativeG(byte[] frame);
    private native String nativeH(int i);
    private native boolean nativeI(Context context);

    public static void init(Context context) {
        if (context != null && sLoaded) {
            try {
                INSTANCE.nativeI(context.getApplicationContext());
            } catch (Throwable t) {
                Log.e(TAG, "Error in nativeI initialization", t);
            }
        }
    }

    public byte[] f(byte[] frame) {
        if (!sLoaded) return null;
        try {
            return nativeA(frame);
        } catch (Throwable t) {
            return null;
        }
    }

    public byte[] g() {
        if (!sLoaded) return null;
        try {
            return nativeD();
        } catch (Throwable t) {
            return null;
        }
    }

    public String h(byte[] frame) {
        if (!sLoaded) return null;
        try {
            return nativeG(frame);
        } catch (Throwable t) {
            return null;
        }
    }

    public static UUID getUuid() {
        if (!sLoaded) return UUID.randomUUID();
        try {
            return UUID.fromString(INSTANCE.nativeH(1));
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }
}
