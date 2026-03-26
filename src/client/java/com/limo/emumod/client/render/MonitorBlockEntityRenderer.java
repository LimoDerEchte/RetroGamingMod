package com.limo.emumod.client.render;

import com.limo.emumod.client.network.ScreenManager;
import com.limo.emumod.monitor.MonitorBlockEntity;
import com.limo.emumod.registry.EmuBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import static com.limo.emumod.client.EmuModClient.mc;

public class MonitorBlockEntityRenderer implements BlockEntityRenderer<MonitorBlockEntity, BlockEntityRenderState> {

    public MonitorBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void submit(BlockEntityRenderState state, @NonNull PoseStack matrices, @NonNull SubmitNodeCollector queue, @NonNull CameraRenderState cameraState) {
        assert mc.level != null;
        if(!(mc.level.getBlockEntity(state.blockPos) instanceof MonitorBlockEntity entity))
            return;

        // Base Model
        float rotation = state.blockState.getValue(BlockStateProperties.ROTATION_16) * 22.5f;
        if(rotation != 0) {
            matrices.pushPose();
            matrices.translate(.5D, .5D, .5D);
            matrices.mulPose(Axis.YP.rotationDegrees(rotation));
            matrices.translate(-.5D, -.5D, -.5D);

            queue.submitBlock(matrices, state.blockState.getBlock().defaultBlockState(),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, 0x00000000);
            matrices.popPose();
        }

        // Content Loading
        UUID file = entity.consoleId;
        if(file == null)
            return;

        @SuppressWarnings("resource")
        DynamicTexture tex = ScreenManager.retrieveDisplay(file);
        if(tex == null)
            return;
        Identifier texId = ScreenManager.texFromUUID(file);

        // Content Render
        assert texId != null;
        queue.submitCustomGeometry(matrices, RenderTypes.entityTranslucent(texId), (_, vertexConsumer) -> {
            matrices.pushPose();
            Vec3 pos = entity.getBlockPos().getCenter().subtract(cameraState.pos);
            matrices.translate(pos);

            matrices.mulPose(Axis.YP.rotationDegrees(rotation));

            matrices.translate(-.5D, -.5D, -.5D);
            if(state.blockState.getBlock() == EmuBlocks.MONITOR)
                matrices.mulPose(Axis.XP.rotationDegrees(22.5f));
            matrices.translate(.5D, .5D, .5D);

            // CONVERSION CALCULATION (Blockbench Cube)
            // z : z - 8  y1: y - 7 + h / 2

            assert tex.getPixels() != null;
            if(state.blockState.getBlock() == EmuBlocks.MONITOR) {
                renderFace(3, 14, 10, -5, tex.getPixels().getWidth(), tex.getPixels().getHeight(),
                        vertexConsumer, state.lightCoords, matrices);
            } else {
                renderFace(6.5f, 44, 25, -1, tex.getPixels().getWidth(), tex.getPixels().getHeight(),
                        vertexConsumer, state.lightCoords, matrices);
            }

            matrices.popPose();
        });
    }

    private static void renderFace(float y, float w, float h, float z, float oW, float oH, VertexConsumer consumer, int light, PoseStack stack) {
        float sourceRatio = oW / oH;
        float ratio = w / h;

        float fW = 1.0f;
        float fH = 1.0f;

        if(sourceRatio < ratio) {
            fW = sourceRatio / ratio; // height shrinks
        } else {
            fH = ratio / sourceRatio; // width shrinks
        }

        Matrix4f modelMatrix = stack.last().pose();
        PoseStack.Pose normalMatrix = stack.last();
        float _x1 = (-w / 2 * fW) / 16F;
        float _y1 = (y - h / 2 * fH) / 16F;
        float _x2 = (w / 2 * fW) / 16F;
        float _y2 = (y + h / 2 * fH) / 16F;
        float _z = (z - 0.01F) / 16F;
        consumer.addVertex(modelMatrix, _x1, _y1, _z).setColor(255, 255, 255, 255).setUv
                (1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(normalMatrix, 0.0f, 0.0f, 1.0f);
        consumer.addVertex(modelMatrix, _x2, _y1, _z).setColor(255, 255, 255, 255).setUv
                (0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(normalMatrix, 0.0f, 0.0f, 1.0f);
        consumer.addVertex(modelMatrix, _x2, _y2, _z).setColor(255, 255, 255, 255).setUv
                (0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(normalMatrix, 0.0f, 0.0f, 1.0f);
        consumer.addVertex(modelMatrix, _x1, _y2, _z).setColor(255, 255, 255, 255).setUv
                (1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(normalMatrix, 0.0f, 0.0f, 1.0f);
    }
}
