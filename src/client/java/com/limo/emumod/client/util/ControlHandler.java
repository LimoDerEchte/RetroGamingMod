package com.limo.emumod.client.util;

import com.limo.emumod.client.bridge.NativeClient;

import java.util.Map;


public class ControlHandler {
    private final Map<Integer, Short> keyMap;
    private final int link;
    private final short port;
    private short input;

    public ControlHandler(Map<Integer, Short> keyMap, int link, short port) {
        this.keyMap = keyMap;
        this.link = link;
        this.port = port;
    }

    public boolean down(int key) {
        if(!keyMap.containsKey(key))
            return false;
        input |= keyMap.get(key);
        if(NativeClient.isConnected())
            NativeClient.updateControls(link, port, input);
        return true;
    }

    public boolean up(int key) {
        if(!keyMap.containsKey(key))
            return false;
        input &= (short) ~keyMap.get(key);
        if(NativeClient.isConnected())
            NativeClient.updateControls(link, port, input);
        return true;
    }
}
