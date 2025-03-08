package com.limo.emumod.client.bridge;

public class NativeClient {
    private final long handle;

    public NativeClient(int port, String token) {
        handle = connect(port, token);
    }

    public void disconnect() {
        disconnect(handle);
    }

    public void poll() {
        poll(handle);
    }

    public boolean isAuthenticated() {
        return isAuthenticated(handle);
    }

    private static native long connect(int port, String token);
    private static native void disconnect(long ptr);
    private static native boolean isAuthenticated(long ptr);
    private static native void poll(long ptr);
}
