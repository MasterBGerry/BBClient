package dev.bbclient.client.protocol;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Combat action payload sent from client to server
 */
public record BBCombatPayload(byte actionType, int targetEntityId, double distance) implements CustomPayload {

    public static final Id<BBCombatPayload> PAYLOAD_ID = new Id<>(BBProtocol.CHANNEL_COMBAT);

    public static final PacketCodec<PacketByteBuf, BBCombatPayload> CODEC = PacketCodec.of(
            BBCombatPayload::write,
            BBCombatPayload::read
    );

    public static BBCombatPayload read(PacketByteBuf buf) {
        byte actionType = buf.readByte();
        int targetEntityId = buf.readVarInt();
        double distance = buf.readDouble();
        return new BBCombatPayload(actionType, targetEntityId, distance);
    }

    public void write(PacketByteBuf buf) {
        buf.writeByte(actionType);
        buf.writeVarInt(targetEntityId);
        buf.writeDouble(distance);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
