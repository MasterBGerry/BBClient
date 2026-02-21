package dev.bbclient.client.protocol;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Handshake payload sent from client to server
 */
public record BBHelloPayload(int modVersion, int supportedFeatures) implements CustomPayload {

    public static final Id<BBHelloPayload> PAYLOAD_ID = new Id<>(BBProtocol.CHANNEL_HANDSHAKE);

    public static final PacketCodec<PacketByteBuf, BBHelloPayload> CODEC = PacketCodec.of(
            BBHelloPayload::write,
            BBHelloPayload::read
    );

    public static BBHelloPayload read(PacketByteBuf buf) {
        int modVersion = buf.readVarInt();
        int supportedFeatures = buf.readVarInt();
        return new BBHelloPayload(modVersion, supportedFeatures);
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(modVersion);
        buf.writeVarInt(supportedFeatures);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
