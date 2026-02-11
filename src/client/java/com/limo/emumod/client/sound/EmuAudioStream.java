package com.limo.emumod.client.sound;

import com.limo.emumod.client.EmuModClient;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sound.BufferedAudioStream;

import javax.sound.sampled.AudioFormat;
import java.util.UUID;

public class EmuAudioStream implements BufferedAudioStream {
    private final AudioFormat format;
    private final UUID uuid;

    public EmuAudioStream(int sampleRate, UUID uuid) {
        this.format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, 2, 2, sampleRate, false);
        this.uuid = uuid;
    }

    @Override
    public boolean read(FloatConsumer consumer) {
        float[] buffer = EmuModClient.CLIENT.lastAudioData(uuid);
        if(buffer.length == 0)
            return false;
        for(float value : buffer) {
            consumer.accept(value);
        }
        return true;
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public void close() { }
}
