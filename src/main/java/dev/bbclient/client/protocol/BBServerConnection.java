package dev.bbclient.client.protocol;

import dev.bbclient.client.BBClientMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Handles network communication with BadBuck server
 */
public class BBServerConnection {

    private static boolean connectedToBBServer = false;
    private static int serverFeatures = 0;
    private static int sprintResetDelayTicks = 0;
    private static int invulnerabilityTicks = 0;
    private static double critMultiplier = 1.5;

    public static void init() {
        registerPayloads();
        registerHandlers();
        registerConnectionEvents();
        BBClientMod.LOGGER.info("BB network handler initialized");
    }

    private static void registerPayloads() {
        // Client to Server
        PayloadTypeRegistry.playC2S().register(BBHelloPayload.PAYLOAD_ID, BBHelloPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BBCombatPayload.PAYLOAD_ID, BBCombatPayload.CODEC);

        // Server to Client
        PayloadTypeRegistry.playS2C().register(BBConfigPayload.PAYLOAD_ID, BBConfigPayload.CODEC);
    }

    private static void registerHandlers() {
        // Handle config from server
        ClientPlayNetworking.registerGlobalReceiver(BBConfigPayload.PAYLOAD_ID, (payload, context) -> {
            handleServerConfig(payload);
        });
    }

    private static void registerConnectionEvents() {
        // On join, send handshake
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sendHandshake();
        });

        // On disconnect, reset state
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            connectedToBBServer = false;
            serverFeatures = 0;
            BBClientMod.LOGGER.info("Disconnected from server, BB features reset");
        });
    }

    private static void handleServerConfig(BBConfigPayload payload) {
        connectedToBBServer = true;
        serverFeatures = payload.enabledFeatures();
        sprintResetDelayTicks = payload.sprintDelayTicks();
        invulnerabilityTicks = payload.invulnTicks();
        critMultiplier = payload.critMultiplier();

        BBClientMod.LOGGER.info("Connected to BB server!");
        BBClientMod.LOGGER.info("Server features: 0x{}, sprintDelay={}, invuln={}, crit={}",
                Integer.toHexString(serverFeatures), sprintResetDelayTicks, invulnerabilityTicks, critMultiplier);
    }

    public static void sendHandshake() {
        // Always try to send handshake - don't check canSend() because Paper server
        // doesn't register Fabric channels, but still handles the packets
        try {
            BBHelloPayload payload = new BBHelloPayload(BBProtocol.MOD_VERSION, BBProtocol.ALL_FEATURES);
            ClientPlayNetworking.send(payload);
            BBClientMod.LOGGER.info("Sent BB handshake (v{}, features=0x{})",
                    BBProtocol.MOD_VERSION, Integer.toHexString(BBProtocol.ALL_FEATURES));
        } catch (Exception e) {
            BBClientMod.LOGGER.warn("Failed to send BB handshake: {}", e.getMessage());
        }
    }

    public static void sendSprintReset() {
        if (ClientPlayNetworking.canSend(BBCombatPayload.PAYLOAD_ID)) {
            BBCombatPayload payload = new BBCombatPayload(BBProtocol.COMBAT_SPRINT_RESET, 0, 0);
            ClientPlayNetworking.send(payload);
        }
    }

    public static void sendCombatAction(byte actionType, int entityId, double distance) {
        if (ClientPlayNetworking.canSend(BBCombatPayload.PAYLOAD_ID)) {
            BBCombatPayload payload = new BBCombatPayload(actionType, entityId, distance);
            ClientPlayNetworking.send(payload);
        }
    }

    public static boolean isConnectedToBBServer() {
        return connectedToBBServer;
    }

    public static int getServerFeatures() {
        return serverFeatures;
    }

    public static boolean hasFeature(int feature) {
        return (serverFeatures & feature) != 0;
    }

    public static int getSprintResetDelayTicks() {
        return sprintResetDelayTicks;
    }

    public static int getInvulnerabilityTicks() {
        return invulnerabilityTicks;
    }

    public static double getCritMultiplier() {
        return critMultiplier;
    }
}
