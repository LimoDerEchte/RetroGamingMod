package com.limo.emumod.client.render;

import com.limo.emumod.monitor.MonitorBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class MonitorBlockEntityRenderer implements BlockEntityRenderer<MonitorBlockEntity> {
    private static final Identifier GAMEBOY_TEXTURE = Identifier.of("emumod", "textures/block/monitor.png");

    public MonitorBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(MonitorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();

        RenderLayer screenLayer = RenderLayer.getEntityTranslucent(GAMEBOY_TEXTURE);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(screenLayer);

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

        vertexConsumer.vertex(modelMatrix, 1/16f, 3/8f, 2.999f/16f).color(255, 255, 255, 255)
                .texture(0.0f, 1.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        vertexConsumer.vertex(modelMatrix, 15/16f, 3/8f, 2.999f/16f).color(255, 255, 255, 255)
                .texture(1.0f, 1.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        vertexConsumer.vertex(modelMatrix, 15/16f, 1f, 2.999f/16f).color(255, 255, 255, 255)
                .texture(1.0f, 0.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        vertexConsumer.vertex(modelMatrix, 1/16f, 1f, 2.999f/16f).color(255, 255, 255, 255)
                .texture(0.0f, 0.0f).overlay(overlay).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        matrices.pop();
    }
}
