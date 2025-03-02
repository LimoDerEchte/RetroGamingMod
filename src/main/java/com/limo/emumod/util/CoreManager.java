package com.limo.emumod.util;

import com.limo.emumod.EmuMod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CoreManager {
    private static final String baseUrl = "https://buildbot.libretro.com/nightly/linux/x86_64/latest/";
    public static File mGBA;

    public static void init() {
        mGBA = FileUtil.getRequiredFile("mgba_libretro.so");
        checkFile(mGBA);
    }

    private static void checkFile(File file) {
        if(file.exists())
            return;
        String dl = baseUrl + file.getName() + ".zip";
        try(ZipInputStream zis = new ZipInputStream(new URI(dl).toURL().openStream());
            FileOutputStream stream = new FileOutputStream(file)) {
            ZipEntry entry = zis.getNextEntry();
            if(entry != null) {
                byte[] buffer = new byte[8192];
                int len;
                while((len = zis.read(buffer)) > 0) {
                    stream.write(buffer, 0, len);
                }
                stream.flush();
            }
        } catch (IOException e) {
            EmuMod.LOGGER.error("Failed to download core", e);
        } catch (URISyntaxException ignore) { }
    }
}
