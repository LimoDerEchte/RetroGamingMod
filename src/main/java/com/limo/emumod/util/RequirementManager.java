package com.limo.emumod.util;

import com.limo.emumod.EmuMod;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RequirementManager {
    private static String baseUrl = "https://buildbot.libretro.com/nightly/";

    public static File bridge;
    public static File core;

    public static File gearBoy;
    public static File beetleGBA;
    public static File genesisPlusGX;
    public static File FCEUmm;
    public static File bsnes;

    public static boolean init() {
        detectPlatform();
        // Required Libraries
        if(!checkFileLocal(bridge, false)
            || !checkFileLocal(core, true))
            return false;
        // LibRetro Cores
        checkCore(gearBoy);
        checkCore(beetleGBA);
        checkCore(genesisPlusGX);
        checkCore(FCEUmm);
        checkCore(bsnes);
        // Load Bridge Lib
        System.load(bridge.getAbsolutePath());
        return true;
    }

    private static void detectPlatform() {
        // Required Libraries
        bridge = FileUtil.getRequiredFile(PlatformDetector.getLibraryName(true, "bridge"));
        core = FileUtil.getRequiredFile(PlatformDetector.getExecutableName("retro-core"));
        // LibRetro Cores
        gearBoy = FileUtil.getRequiredFile(PlatformDetector.getLibraryName(false, "gearboy_libretro"));
        beetleGBA = FileUtil.getRequiredFile(PlatformDetector.getLibraryName(false, "mednafen_gba_libretro"));
        genesisPlusGX = FileUtil.getRequiredFile(PlatformDetector.getLibraryName(false, "genesis_plus_gx_libretro"));
        FCEUmm = FileUtil.getRequiredFile(PlatformDetector.getLibraryName(false, "fceumm_libretro"));
        bsnes = FileUtil.getRequiredFile(PlatformDetector.getLibraryName(false, "bsnes_libretro"));
        // Update platform download base url
        String arch = PlatformDetector.is64Bit() ? "x86_64" : "x86";
        String platform = PlatformDetector.isWindows() ? "windows" : "linux";
        baseUrl += platform + "/" + arch + "/latest/";
    }

    private static void checkCore(File file) {
        if(file.exists())
            return;
        String dl = baseUrl + file.getName() + ".zip";
        EmuMod.LOGGER.info("Downloading {} from {}", file.getName(), dl);
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

    private static boolean checkFileLocal(File file, boolean setExec) {
        if(file.exists())
            return true;
        EmuMod.LOGGER.info("Extracting {}", file.getName());
        URL url = RequirementManager.class.getResource("/lib/" + file.getName());
        if(url == null) {
            EmuMod.LOGGER.error("Failed to find vital library");
            return false;
        }
        try(InputStream is = url.openStream(); OutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            os.flush();
            if(setExec) {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file.toPath(), LinkOption.NOFOLLOW_LINKS);
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
                permissions.add(PosixFilePermission.GROUP_EXECUTE);
                permissions.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(file.toPath(), permissions);
            }
        } catch (IOException e) {
            EmuMod.LOGGER.error("Failed to extract vital library", e);
            return false;
        }
        return true;
    }
}
