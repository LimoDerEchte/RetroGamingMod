package com.limo.emumod.client.network;

import com.limo.emumod.EmuMod;
import com.limo.emumod.client.screen.CartridgeScreen;
import com.limo.emumod.client.screen.GameboyAdvanceScreen;
import com.limo.emumod.client.screen.GameboyScreen;
import com.limo.emumod.network.NetworkId;
import com.limo.emumod.network.S2C;
import com.limo.emumod.util.VideoCompression;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.texture.NativeImage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.limo.emumod.client.EmuModClient.mc;

public class ClientHandler {
    public static Map<UUID, NativeImage> displayBuffer = new HashMap<>();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenScreenPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> ctx.client().setScreen(switch (payload.type()) {
                    case NetworkId.ScreenType.CARTRIDGE -> new CartridgeScreen();
            default -> throw new AssertionError();
        })));
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenGameScreenPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> ctx.client().setScreen(switch (payload.type()) {
                    case NetworkId.ScreenType.GAMEBOY -> new GameboyScreen(false, payload.fileId());
                    case NetworkId.ScreenType.GAMEBOY_COLOR -> new GameboyScreen(true, payload.fileId());
                    case NetworkId.ScreenType.GAMEBOY_ADVANCE -> new GameboyAdvanceScreen(payload.fileId());
                    default -> throw new AssertionError();
        })));
        ClientPlayNetworking.registerGlobalReceiver(S2C.CloseScreenPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
            if(mc.currentScreen instanceof CartridgeScreen screen && screen.handle == payload.handle())
                screen.close();
        }));
        ClientPlayNetworking.registerGlobalReceiver(S2C.UpdateDisplayDataPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
            if(!displayBuffer.containsKey(payload.uuid()))
                displayBuffer.put(payload.uuid(), switch (payload.type()) {
                    case NetworkId.DisplaySize.w160h144 -> new NativeImage(160, 144, false);
                    case NetworkId.DisplaySize.w240h160 -> new NativeImage(240, 160, false);
                    default -> throw new AssertionError();
            });
            NativeImage img = displayBuffer.get(payload.uuid());
            try {
                int[] display = VideoCompression.decompress(payload.data());
                int height = img.getHeight();
                int width = img.getWidth();
                for(int y = 0; y < height; y++) {
                    for(int x = 0; x < width; x++) {
                        img.setColorArgb(x, y, display[y * width + x]);
                    }
                }
            } catch (IOException e) {
                EmuMod.LOGGER.error("Failed to decompress display image", e);
            }
        }));
    }
}
