package com.limo.emumod.client.bridge;

public class NativeClient {
    private final long handle;

    public NativeClient(int port) {
        handle = connect(port);
    }

    public void disconnect() {
        disconnect(handle);
    }

    public void poll() {
        poll(handle);
    }

    private static native long connect(int port);
    private static native void disconnect(long ptr);
    private static native void poll(long ptr);
}
