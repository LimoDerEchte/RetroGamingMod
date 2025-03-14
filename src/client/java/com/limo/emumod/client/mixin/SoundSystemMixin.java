package com.limo.emumod.client.mixin;

import com.limo.emumod.client.network.ClientHandler;
import com.limo.emumod.client.util.BufferedAudioOutput;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        ClientHandler.audioBuffer.values().forEach(BufferedAudioOutput::updatePositions);
    }
}
