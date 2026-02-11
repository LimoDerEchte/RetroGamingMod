package com.limo.emumod.client.sound;

import com.limo.emumod.client.EmuModClient;
import com.limo.emumod.client.mixin.SoundManagerAccessor;
import com.limo.emumod.client.mixin.SoundSystemAccessor;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.sound.Source;

import java.util.UUID;

import static com.limo.emumod.client.EmuModClient.mc;

public class EmuSpeaker {
    private final Source source;

    protected final int sampleRate;
    protected final UUID deviceId;

    public EmuSpeaker(UUID deviceId, int sampleRate) {
        this.sampleRate = sampleRate;
        this.deviceId = deviceId;

        SoundEngine engine = ((SoundSystemAccessor) ((SoundManagerAccessor)
                mc.getSoundManager()).emumod$soundSystem()).emumod$soundEngine();
        this.source = engine.createSource(SoundEngine.RunMode.STREAMING);
    }

    public void open() {
        source.setStream(new EmuAudioStream(sampleRate, deviceId));
        new Thread(() -> {
            while (EmuModClient.CLIENT.lastAudioData(deviceId).length == 0)
                Thread.yield();
            mc.execute(source::play);
        }).start();
    }
}
