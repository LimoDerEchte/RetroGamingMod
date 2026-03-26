package com.limo.emumod.client.sound;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.FloatSampleSource;
import java.util.UUID;

public class EmuAudioStream implements FloatSampleSource {
    private final AudioFormat format;
    private final UUID uuid;

    public EmuAudioStream(int sampleRate, UUID uuid) {
        this.format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, 2, 2, sampleRate, false);
        this.uuid = uuid;
    }

    @Override
    public boolean readChunk(FloatConsumer consumer) {
        // TODO: Reimplement if streaming over java is possible
        float[] buffer = new float[0]; // EmuModClient.CLIENT.lastAudioData(uuid);
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
