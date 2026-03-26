package com.limo.emumod.client.sound;

//import com.limo.emumod.client.mixin.SoundManagerAccessor;
//import com.limo.emumod.client.mixin.SoundSystemAccessor;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import java.util.UUID;

import static com.limo.emumod.client.EmuModClient.mc;

public class EmuSpeaker {
    //private final Channel source;

    protected final int sampleRate;
    protected final UUID deviceId;

    public EmuSpeaker(UUID deviceId, int sampleRate) {
        this.sampleRate = sampleRate;
        this.deviceId = deviceId;

        //Library engine = ((SoundSystemAccessor) ((SoundManagerAccessor)
        //        mc.getSoundManager()).emumod$soundSystem()).emumod$soundEngine();
        //this.source = engine.acquireChannel(Library.Pool.STREAMING);
    }

    public void open() {
        //source.attachBufferStream(new EmuAudioStream(sampleRate, deviceId));
        new Thread(() -> {
            //while (EmuModClient.CLIENT.lastAudioData(deviceId).length == 0)
            //    Thread.yield();
            //mc.execute(source::play);
        }).start();
    }
}
