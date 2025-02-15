package com.limo.emumod.client.util;

import com.limo.emumod.util.FileUtil;
import net.minecraft.util.WorldSavePath;

import java.util.Objects;

import static com.limo.emumod.client.EmuModClient.mc;

public class ClientFilePath implements FileUtil.IClientPath {

    public static void init() {
        FileUtil.clientPath = new ClientFilePath();
    }

    @Override
    public String getSavePath() {
        String savePath = Objects.requireNonNull(mc.getServer()).getSavePath(WorldSavePath.ROOT).toString();
        if (savePath.lastIndexOf('.') == savePath.length() - 1) {
            savePath = savePath.substring(0, savePath.length() - 1);
        }
        return savePath;
    }
}
