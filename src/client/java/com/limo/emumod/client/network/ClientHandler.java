package com.limo.emumod.client.network;

import com.limo.emumod.client.bridge.NativeClient;
import com.limo.emumod.client.screen.*;
import com.limo.emumod.network.NetworkId;
import com.limo.emumod.network.S2C;
import com.limo.emumod.util.AudioCodec;
import com.limo.emumod.util.VideoCodec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import static com.limo.emumod.client.EmuModClient.mc;

public class ClientHandler {

    public static void init() {
        // Protocol Stuff
        ClientPlayNetworking.registerGlobalReceiver(S2C.EmuModTokenPayload.ID, (payload, ctx) -> ctx.client().execute(() -> NativeClient.init(payload.token())));
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> NativeClient.deinit());
        // Screen Stuff
        //noinspection SwitchStatementWithTooFewBranches
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenScreenPayload.ID, (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(switch (payload.type()) {
                    case NetworkId.ScreenType.CARTRIDGE -> new CartridgeScreen();
                    default -> throw new AssertionError();
        })));
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenGameScreenPayload.ID, (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(switch (payload.type()) {
                    case NetworkId.ScreenType.GAMEBOY -> new GameboyScreen(false, payload.streamId());
                    case NetworkId.ScreenType.GAMEBOY_COLOR -> new GameboyScreen(true, payload.streamId());
                    case NetworkId.ScreenType.GAMEBOY_ADVANCE -> new GameboyAdvanceScreen(payload.streamId());
                    case NetworkId.ScreenType.GAME_GEAR -> new GameGearScreen(payload.streamId());
                    case NetworkId.ScreenType.CONTROLLER -> new RawControllerScreen(payload.streamId(), (short) payload.port().orElse(0));
                    default -> throw new AssertionError();
        })));
        ClientPlayNetworking.registerGlobalReceiver(S2C.CloseScreenPayload.ID, (payload, ctx) -> ctx.client().execute(() -> {
            if(mc.currentScreen instanceof CartridgeScreen screen && screen.handle == payload.handle())
                screen.close();
        }));
        // Other Stuff
        ClientPlayNetworking.registerGlobalReceiver(S2C.UpdateEmulatorPayload.ID, (payload, ctx) -> ctx.client().execute(() -> {
            int width = payload.width();
            int height = payload.height();
            VideoCodec videoCodec = VideoCodec.values()[payload.videoCodec()];
            AudioCodec audioCodec = AudioCodec.values()[payload.audioCodec()];

            if(width == 0 || height == 0) {
                ScreenManager.unregisterDisplay(payload.id());
            } else {
                ScreenManager.registerDisplay(payload.console(), payload.id(), width, height, videoCodec, audioCodec);
            }
        }));
    }
}
