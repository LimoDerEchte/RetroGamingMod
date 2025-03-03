package com.limo.emumod.client.util;

import net.minecraft.client.texture.NativeImage;

public class NativeImageRatio {
    private final NativeImage nativeImage;

    public NativeImageRatio(int width, int height, int rW, int rH) {
        if(width / rW > height / rH) {
            height = width * rH / rW;
        } else {
            width = height * rW / rH;
        }
        nativeImage = new NativeImage(width, height, false);
    }

    public boolean matches(NativeImage image) {
        return nativeImage.getWidth() == image.getWidth() || nativeImage.getHeight() == image.getHeight();
    }

    public void readFrom(NativeImage image) {
        int offX = (nativeImage.getWidth() - image.getWidth()) / 2;
        int offY = (nativeImage.getHeight() - image.getHeight()) / 2;
        nativeImage.fillRect(0, 0, nativeImage.getWidth(), nativeImage.getHeight(), 0x00000000);
        image.copyRect(nativeImage, 0, 0, offX, offY, image.getWidth(), image.getHeight(), false, false);
    }

    public NativeImage getImage() {
        return nativeImage;
    }
}
