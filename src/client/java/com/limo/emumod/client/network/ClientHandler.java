package com.limo.emumod.client.network;

import com.limo.emumod.client.bridge.NativeClient;
import com.limo.emumod.client.screen.CartridgeScreen;
import com.limo.emumod.client.screen.GameGearScreen;
import com.limo.emumod.client.screen.GameboyAdvanceScreen;
import com.limo.emumod.client.screen.GameboyScreen;
import com.limo.emumod.network.NetworkId;
import com.limo.emumod.network.S2C;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.Objects;

import static com.limo.emumod.client.EmuModClient.CLIENT;
import static com.limo.emumod.client.EmuModClient.mc;

public class ClientHandler {

    private static String ip() {
        String address = Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getConnection().getAddressAsString(true);
        if(address.startsWith("local") || address.isEmpty())
            return "127.0.0.1";
        return address;
    }

    public static void init() {
        // ENet Stuff
        ClientPlayNetworking.registerGlobalReceiver(S2C.ENetTokenPayload.ID, (payload, ctx) -> ctx.client().execute(() -> {
            CLIENT = new NativeClient(ip(), payload.port(), payload.token());
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if(CLIENT != null) {
                CLIENT.disconnect();
                CLIENT = null;
            }
        });
        // Screen Stuff
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenScreenPayload.ID, (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(switch (payload.type()) {
                    case NetworkId.ScreenType.CARTRIDGE -> new CartridgeScreen();
                    default -> throw new AssertionError();
        })));
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenGameScreenPayload.ID, (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(switch (payload.type()) {
                    case NetworkId.ScreenType.GAMEBOY -> new GameboyScreen(false, payload.fileId());
                    case NetworkId.ScreenType.GAMEBOY_COLOR -> new GameboyScreen(true, payload.fileId());
                    case NetworkId.ScreenType.GAMEBOY_ADVANCE -> new GameboyAdvanceScreen(payload.fileId());
                    case NetworkId.ScreenType.GAME_GEAR -> new GameGearScreen(payload.fileId());
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
            int sampleRate = payload.sampleRate();
            if(width == 0 || height == 0 || sampleRate == 0) {
                ScreenManager.unregisterDisplay(payload.uuid());
            } else {
                ScreenManager.registerDisplay(payload.uuid(), width, height, sampleRate);
            }
        }));
    }
}
