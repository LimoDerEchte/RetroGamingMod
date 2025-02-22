package com.limo.emumod.util;

import com.limo.emumod.EmuMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.util.UUID;

public class FileUtil {
    public static IClientPath clientPath;

    public static File idToFile(UUID uuid, String extension) {
        String parent = clientPath == null ? getSavePath() : clientPath.getSavePath();
        return new File(parent + File.separator + "cartridges", uuid + "." + extension);
    }

    public static File getRequiredFile(String name) {
        return new File(getSavePath() + File.separator + "requiredFiles", name);
    }

    public static void initGeneric() {
        File f = new File(getSavePath() + File.separator + "requiredFiles");
        if(!f.exists())
            if(!f.mkdir())
                EmuMod.LOGGER.error("Failed to create requiredFiles folder");
    }

    public static void init() {
        String parent = clientPath == null ? getSavePath() : clientPath.getSavePath();
        File f = new File(parent, "cartridges");
        if(!f.exists())
            if(!f.mkdir())
                EmuMod.LOGGER.error("Failed to create cartridges folder");
    }

    public static String getSavePath() {
        String savePath = FabricLoader.getInstance().getGameDir().resolve(WorldSavePath.ROOT.getRelativePath()).toString();
        if (savePath.lastIndexOf('.') == savePath.length() - 1) {
            savePath = savePath.substring(0, savePath.length() - 1);
        }
        return savePath;
    }

    public interface IClientPath {
        String getSavePath();
    }
}
