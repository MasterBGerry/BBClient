package dev.bbclient.client;

import dev.bbclient.client.combat.ClientCombatHandler;
import dev.bbclient.client.protocol.BBServerConnection;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BBClientMod implements ClientModInitializer {
    public static final String MOD_ID = "bbclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("BBClient initializing - 1.7/1.8 combat mod for Badbuck.net");

        // Initialize network protocol
        BBServerConnection.init();

        // Register tick handler for combat mechanics
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientCombatHandler.tick();
        });

        LOGGER.info("BBClient initialized!");
        LOGGER.info("Features: Sword Blocking, Sprint Reset, Block & Hit, Extended Hitbox");
    }
}
