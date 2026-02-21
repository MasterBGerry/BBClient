package dev.bbclient.client.protocol;

import net.minecraft.util.Identifier;

/**
 * Protocol constants for BBClient-Server communication
 */
public final class BBProtocol {
    public static final String NAMESPACE = "baper";

    // Channel identifiers
    public static final Identifier CHANNEL_HANDSHAKE = Identifier.of(NAMESPACE, "handshake");
    public static final Identifier CHANNEL_CONFIG = Identifier.of(NAMESPACE, "config");
    public static final Identifier CHANNEL_COMBAT = Identifier.of(NAMESPACE, "combat");

    // Protocol version
    public static final int PROTOCOL_VERSION = 1;
    public static final int MOD_VERSION = 1;

    // Feature flags (bitmask)
    public static final int FEATURE_SPRINT_RESET = 1;      // 0x01
    public static final int FEATURE_EXTENDED_HITBOX = 2;   // 0x02
    public static final int FEATURE_NO_COOLDOWN = 4;       // 0x04
    public static final int FEATURE_SWORD_BLOCKING = 8;    // 0x08
    public static final int FEATURE_LEGACY_CRITS = 16;     // 0x10
    public static final int FEATURE_LEGACY_KNOCKBACK = 32; // 0x20

    // All features
    public static final int ALL_FEATURES = FEATURE_SPRINT_RESET | FEATURE_EXTENDED_HITBOX |
            FEATURE_NO_COOLDOWN | FEATURE_SWORD_BLOCKING |
            FEATURE_LEGACY_CRITS | FEATURE_LEGACY_KNOCKBACK;

    // Combat action types
    public static final byte COMBAT_ATTACK = 0x10;
    public static final byte COMBAT_SPRINT_RESET = 0x11;

    private BBProtocol() {}
}
