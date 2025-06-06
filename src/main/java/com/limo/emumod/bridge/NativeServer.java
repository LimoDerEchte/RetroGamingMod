package com.limo.emumod.bridge;

public class NativeServer {
    private final long handle;
    private final int port;

    public NativeServer(int port, int maxUsers) {
        handle = startServer(port, maxUsers);
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void stop() {
        stopServer(handle);
    }

    public String createToken() {
        return requestToken(handle);
    }

    private static native long startServer(int port, int maxUsers);
    private static native void stopServer(long ptr);
    private static native String requestToken(long ptr);
}
