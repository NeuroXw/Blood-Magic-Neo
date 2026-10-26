package wayoftime.bloodmagic.test;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload;

/** Runs the real ServerPlayer.openMenu/initMenu and container-click handler, without a socket. */
final class MenuTestPlayer {
    static ServerPlayer create(ServerLevel level, String name) {
        var profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name);
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        player.connection = new CapturingConnection(level, player);
        player.connection.markClientLoaded();
        return player;
    }

    static CapturingConnection connection(ServerPlayer player) { return (CapturingConnection) player.connection; }

    static final class CapturingConnection extends ServerGamePacketListenerImpl {
        final List<Packet<?>> packets = new ArrayList<>();

        CapturingConnection(ServerLevel level, ServerPlayer player) {
            super(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            connectionType = ConnectionType.NEOFORGE;
        }

        @Override
        public boolean hasChannel(Identifier id) {
            // Model a completed NeoForge handshake. Production negotiation is not mocked by mod code.
            return id.equals(AdvancedContainerSetDataPayload.TYPE.id());
        }

        @Override
        public void send(Packet<?> packet) { packets.add(packet); }

        @Override
        public void send(Packet<?> packet, ChannelFutureListener listener) { packets.add(packet); }
    }
}
