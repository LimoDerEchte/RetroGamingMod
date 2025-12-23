package com.limo.emumod.client.render;

import com.limo.emumod.client.network.ScreenManager;
import com.limo.emumod.monitor.MonitorBlockEntity;
import com.limo.emumod.registry.EmuBlocks;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.UUID;

import static com.limo.emumod.client.EmuModClient.mc;

public class MonitorBlockEntityRenderer implements BlockEntityRenderer<MonitorBlockEntity, BlockEntityRenderState> {

    public MonitorBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void render(BlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        assert mc.world != null;
        if(!(mc.world.getBlockEntity(state.pos) instanceof MonitorBlockEntity entity))
            return;

        // Base Model
        float rotation = entity.getCachedState().get(Properties.ROTATION) * 22.5f;
        if(rotation != 0) {
            matrices.push();
            matrices.translate(.5D, .5D, .5D);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
            matrices.translate(-.5D, -.5D, -.5D);

            queue.submitBlock(matrices, state.blockState.getBlock().getDefaultState(),
                    state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, 0x00000000);
            matrices.pop();
        }

        // Content Loading
        UUID file = entity.fileId;
        if(file == null)
            return;
        NativeImageBackedTexture tex = ScreenManager.retrieveDisplay(file);
        if(tex == null)
            return;
        Identifier texId = ScreenManager.texFromUUID(file);

        // Content Render
        queue.submitCustom(matrices, RenderLayers.entityTranslucent(texId), (_, vertexConsumer) -> {
            matrices.push();
            Vec3d pos = entity.getPos().toCenterPos().subtract(cameraState.pos);
            matrices.translate(pos);

            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

            matrices.translate(-.5D, -.5D, -.5D);
            if(entity.getCachedState().getBlock() == EmuBlocks.MONITOR)
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(22.5f));
            matrices.translate(.5D, .5D, .5D);

            // CONVERSION CALCULATION (Blockbench Cube)
            // z : z - 8  y1: y - 7 + w / 2

            assert tex.getImage() != null;
            if(entity.getCachedState().getBlock() == EmuBlocks.MONITOR) {
                renderFace(3, 14, 10, -5, tex.getImage().getWidth(), tex.getImage().getHeight(),
                        vertexConsumer, state.lightmapCoordinates, matrices);
            } else {
                renderFace(-6, 44, 19, -1, tex.getImage().getWidth(), tex.getImage().getHeight(),
                        vertexConsumer, state.lightmapCoordinates, matrices);
            }

            matrices.pop();
        });
    }

    private static void renderFace(float y, float w, float h, float z, float oW, float oH, VertexConsumer consumer, int light, MatrixStack stack) {
        float sourceRatio = oW / oH;
        float ratio = w / h;

        float fW = 1.0f;
        float fH = 1.0f;

        if(sourceRatio > ratio) {
            fW = ratio / sourceRatio; // width shrinks
        } else {
            fH = sourceRatio / ratio; // height shrinks
        }

        Matrix4f modelMatrix = stack.peek().getPositionMatrix();
        MatrixStack.Entry normalMatrix = stack.peek();
        float _x1 = (-w / 2 * fW) / 16F;
        float _y1 = (y - y / 2 * fH) / 16F;
        float _x2 = (w / 2 * fW) / 16F;
        float _y2 = (y + y / 2 * fH) / 16F;
        float _z = (z - 0.01F) / 16F;
        consumer.vertex(modelMatrix, _x1, _y1, _z).color(255, 255, 255, 255).texture
                (1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        consumer.vertex(modelMatrix, _x2, _y1, _z).color(255, 255, 255, 255).texture
                (0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        consumer.vertex(modelMatrix, _x2, _y2, _z).color(255, 255, 255, 255).texture
                (0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
        consumer.vertex(modelMatrix, _x1, _y2, _z).color(255, 255, 255, 255).texture
                (1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, 0.0f, 0.0f, 1.0f);
    }
}
