package com.limo.emumod.bridge;

public class NativeServer {
    private final long handle;

    public NativeServer(int port) {
        handle = startServer(port);
    }

    public void stop() {
        stopServer(handle);
    }

    public void poll() {
        poll(handle);
    }

    private static native long startServer(int port);
    private static native void stopServer(long ptr);
    private static native void poll(long ptr);
}
