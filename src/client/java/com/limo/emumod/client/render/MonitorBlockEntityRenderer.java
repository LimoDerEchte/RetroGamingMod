package com.limo.emumod.client.render;

import com.limo.emumod.client.network.ScreenManager;
import com.limo.emumod.client.network.SoundManager;
import com.limo.emumod.client.util.NativeImageRatio;
import com.limo.emumod.monitor.MonitorBlockEntity;
import com.limo.emumod.registry.EmuBlocks;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.limo.emumod.client.EmuModClient.mc;

public class MonitorBlockEntityRenderer implements BlockEntityRenderer<MonitorBlockEntity> {
    private static final Map<BlockPos, NativeImageRatio> ratioCache = new HashMap<>();
    private static final Map<BlockPos, NativeImageBackedTexture> textureCache = new HashMap<>();
    private static final Map<BlockPos, Identifier> idCache = new HashMap<>();

    public MonitorBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(MonitorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Content
        if(!idCache.containsKey(entity.getPos())) {
            idCache.put(entity.getPos(), Identifier.of("emumod", "monitor_" + entity.getPos().getX() + "_" + entity.getPos().getY() + "_" + entity.getPos().getZ()));
            textureCache.put(entity.getPos(), new NativeImageBackedTexture(new NativeImage(1, 1, false)));
            ratioCache.put(entity.getPos(), new NativeImageRatio(1, 1, 1,  1));
            mc.getTextureManager().registerTexture(idCache.get(entity.getPos()), textureCache.get(entity.getPos()));
        }
        UUID file = entity.fileId;
        if(file == null)
            return;
        double distance = mc.cameraEntity == null ? 0 : mc.cameraEntity.getPos().distanceTo(entity.getPos().toCenterPos());
        SoundManager.updateInRender(file, distance);
        Identifier id = idCache.get(entity.getPos());
        NativeImageBackedTexture tex = textureCache.get(entity.getPos());
        NativeImage newTex = ScreenManager.getDisplay(file);
        NativeImageRatio r = ratioCache.get(entity.getPos());
        if(!r.matches(newTex)) {
            if(entity.getCachedState().getBlock() == EmuBlocks.MONITOR) {
                r = new NativeImageRatio(newTex.getWidth(), newTex.getHeight(), 7, 5);
            } else {
                r = new NativeImageRatio(newTex.getWidth(), newTex.getHeight(), 44, 25);
            }
            ratioCache.put(entity.getPos(), r);
            NativeImageBackedTexture newAlloc = new NativeImageBackedTexture(r.getImage());
            textureCache.put(entity.getPos(), newAlloc);
            mc.getTextureManager().registerTexture(id, newAlloc);
        }
        r.readFrom(newTex);
        tex.upload();
        // Render
        matrices.push();
        switch (entity.getCachedState().get(Properties.HORIZONTAL_FACING)) {
            case EAST -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
                matrices.translate(0, 0, -1);
            }
            case WEST -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                matrices.translate(-1, 0, 0);
            }
            case SOUTH -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                matrices.translate(-1, 0, -1);
            }
        }
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(id));
        if(entity.getCachedState().getBlock() == EmuBlocks.MONITOR) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(22.5f));
            renderFace(1, 6, 15, 16, 3, vertexConsumer, overlay, light, matrices);
        } else {
            renderFace(-14, 2, 30, 27, 7, vertexConsumer, overlay, light, matrices);
        }
        matrices.pop();
    }

    private static void renderFace(int x1, int y1, int x2, int y2, float z, VertexConsumer consumer, int overlay, int light, MatrixStack stack) {
        Matrix4f modelMatrix = stack.peek().getPositionMatrix();
        MatrixStack.Entry normalMatrix = stack.peek();
        float _x1 = x1 / 16F;
        float _y1 = y1 / 16F;
        float _x2 = x2 / 16F;
        float _y2 = y2 / 16F;
        float _z = (z - 0.01F) / 16F;
        consumer.vertex(modelMatrix, _x1, _y1, _z).color(255, 255, 255, 255).texture
                (1.0f, 1.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        consumer.vertex(modelMatrix, _x2, _y1, _z).color(255, 255, 255, 255).texture
                (0.0f, 1.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        consumer.vertex(modelMatrix, _x2, _y2, _z).color(255, 255, 255, 255).texture
                (0.0f, 0.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        consumer.vertex(modelMatrix, _x1, _y2, _z).color(255, 255, 255, 255).texture
                (1.0f, 0.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
    }
}
