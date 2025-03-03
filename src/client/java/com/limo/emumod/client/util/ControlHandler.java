package com.limo.emumod.client.util;

import com.limo.emumod.network.C2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Map;
import java.util.UUID;

public class ControlHandler {
    private final Map<Integer, Short> keyMap;
    private final UUID link;
    private short input;

    public ControlHandler(Map<Integer, Short> keyMap, UUID link) {
        this.keyMap = keyMap;
        this.link = link;
    }

    public boolean down(int key) {
        if(!keyMap.containsKey(key))
            return false;
        input |= keyMap.get(key);
        ClientPlayNetworking.send(new C2S.UpdateGameControls(link, input));
        return true;
    }

    public boolean up(int key) {
        if(!keyMap.containsKey(key))
            return false;
        input &= (short) ~keyMap.get(key);
        ClientPlayNetworking.send(new C2S.UpdateGameControls(link, input));
        return true;
    }
}
