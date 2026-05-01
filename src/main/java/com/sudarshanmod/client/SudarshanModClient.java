package com.sudarshanmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class SudarshanModClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("sudarshanmod");

    @Override
    public void onInitializeClient() {
        // Initialize client-side code here
        LOGGER.info("Sudarshan Chakra client initialization started");
        
        // Add client-side event handlers, renderers, or input handling here
    }
}
