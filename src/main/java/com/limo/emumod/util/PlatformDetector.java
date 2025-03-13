package com.limo.emumod.util;

public class PlatformDetector {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    public static boolean isLinux() {
        return OS_NAME.contains("linux") || OS_NAME.contains("unix");
    }

    public static boolean is64Bit() {
        return OS_ARCH.contains("64") || OS_ARCH.equals("amd64") || OS_ARCH.equals("aarch64");
    }

    public static String getLibraryName(boolean linuxPrefix, String baseName) {
        String prefix = "";
        String suffix = "";
        if (isWindows()) {
            suffix = ".dll";
        } else if (isMac()) {
            prefix = "lib";
            suffix = ".dylib";
        } else if (isLinux()) {
            prefix = "lib";
            suffix = ".so";
        }
        if (linuxPrefix)
            return prefix + baseName + (is64Bit() ? "" : "_x86") + suffix;
        return baseName + (is64Bit() ? "" : "_x86") + suffix;
    }

    public static String getExecutableName(String baseName) {
        String suffix = "";
        if (isWindows()) {
            suffix = ".exe";
        }
        return baseName + (is64Bit() ? "" : "_x86") + suffix;
    }
}
