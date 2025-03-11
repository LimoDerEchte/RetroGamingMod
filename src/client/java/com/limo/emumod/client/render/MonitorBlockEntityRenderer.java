package com.limo.emumod.client.render;

import com.limo.emumod.client.network.ScreenManager;
import com.limo.emumod.client.util.NativeImageRatio;
import com.limo.emumod.monitor.MonitorBlockEntity;
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
        Identifier id = idCache.get(entity.getPos());
        NativeImageBackedTexture tex = textureCache.get(entity.getPos());
        NativeImage newTex = ScreenManager.getDisplay(file);
        NativeImageRatio r = ratioCache.get(entity.getPos());
        if(!r.matches(newTex)) {
            r = new NativeImageRatio(newTex.getWidth(), newTex.getHeight(), 7, 5);
            ratioCache.put(entity.getPos(), r);
            NativeImageBackedTexture newAlloc = new NativeImageBackedTexture(r.getImage());
            textureCache.put(entity.getPos(), newAlloc);
            mc.getTextureManager().registerTexture(id, newAlloc);
        }
        r.readFrom(newTex);
        tex.upload();
        // Render
        matrices.push();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(id));
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

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(22.5f));
        Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
        MatrixStack.Entry normalMatrix = matrices.peek();

        vertexConsumer.vertex(modelMatrix, 1/16f, 3/8f, 2.99f/16f).color(255, 255, 255, 255)
                .texture(1.0f, 1.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        vertexConsumer.vertex(modelMatrix, 15/16f, 3/8f, 2.99f/16f).color(255, 255, 255, 255)
                .texture(0.0f, 1.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        vertexConsumer.vertex(modelMatrix, 15/16f, 1f, 2.99f/16f).color(255, 255, 255, 255)
                .texture(0.0f, 0.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        vertexConsumer.vertex(modelMatrix, 1/16f, 1f, 2.99f/16f).color(255, 255, 255, 255)
                .texture(1.0f, 0.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        matrices.pop();
    }
}
