package com.limo.emumod.bridge;

public class NativeServer {
    private final long handle;
    private final int port;

    public NativeServer(int port) {
        handle = startServer(port);
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void stop() {
        stopServer(handle);
    }

    public void poll() {
        poll(handle);
    }

    public String createToken() {
        return requestToken(handle);
    }

    private static native long startServer(int port);
    private static native void stopServer(long ptr);
    private static native String requestToken(long ptr);
    private static native void poll(long ptr);
}
