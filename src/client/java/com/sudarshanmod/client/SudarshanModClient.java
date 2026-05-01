package com.sudarshanmod.client;

import com.sudarshanmod.SudarshanMod;
import com.sudarshanmod.client.renderer.ChakraEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class SudarshanModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(SudarshanMod.CHAKRA_ENTITY, ChakraEntityRenderer::new);
    }
}
