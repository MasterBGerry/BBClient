package dev.bbclient.client.protocol;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Config payload sent from server to client
 */
public record BBConfigPayload(int enabledFeatures, int sprintDelayTicks, int invulnTicks, double critMultiplier) implements CustomPayload {

    public static final Id<BBConfigPayload> PAYLOAD_ID = new Id<>(BBProtocol.CHANNEL_CONFIG);

    public static final PacketCodec<PacketByteBuf, BBConfigPayload> CODEC = PacketCodec.of(
            BBConfigPayload::write,
            BBConfigPayload::read
    );

    public static BBConfigPayload read(PacketByteBuf buf) {
        int enabledFeatures = buf.readVarInt();
        int sprintDelayTicks = buf.readVarInt();
        int invulnTicks = buf.readVarInt();
        double critMultiplier = buf.readDouble();
        return new BBConfigPayload(enabledFeatures, sprintDelayTicks, invulnTicks, critMultiplier);
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(enabledFeatures);
        buf.writeVarInt(sprintDelayTicks);
        buf.writeVarInt(invulnTicks);
        buf.writeDouble(critMultiplier);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
