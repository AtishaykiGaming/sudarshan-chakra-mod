package com.sudarshanmod.client.renderer;

import com.sudarshanmod.SudarshanMod;
import com.sudarshanmod.entity.ChakraEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class ChakraEntityRenderer extends EntityRenderer<ChakraEntity> {

    private static final Identifier TEXTURE =
        Identifier.of(SudarshanMod.MOD_ID, "textures/item/sudarshan_chakra.png");

    private final net.minecraft.client.render.item.ItemRenderer itemRenderer;

    public ChakraEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(ChakraEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // Spin on the Y-axis (flat chakra disc rotating face-up)
        float angle = entity.spinAngle + tickDelta * 18f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));

        // Tilt slightly so it looks like a thrown disc
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        // Scale to a nice in-world size
        matrices.scale(1.5f, 1.5f, 1.5f);

        // Render the chakra as a flat item model
        itemRenderer.renderItem(
            new ItemStack(SudarshanMod.SUDARSHAN_CHAKRA),
            ModelTransformationMode.GROUND,
            light,
            OverlayTexture.DEFAULT_UV,
            matrices,
            vertexConsumers,
            entity.getWorld(),
            entity.getId()
        );

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(ChakraEntity entity) {
        return TEXTURE;
    }
}
