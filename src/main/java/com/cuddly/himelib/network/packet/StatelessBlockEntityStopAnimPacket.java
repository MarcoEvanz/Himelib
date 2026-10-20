package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoBlockEntity;
import com.cuddly.himelib.animatable.stateless.StatelessGeoBlockEntity;
import com.cuddly.himelib.network.packet.MultiloaderPacket;
import com.cuddly.himelib.util.ClientUtil;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record StatelessBlockEntityStopAnimPacket(BlockPos blockPos, String animation) implements MultiloaderPacket {
    public static final Type<StatelessBlockEntityStopAnimPacket> TYPE = new Type<>(GeckoLibConstants.id("stateless_block_entity_stop_anim"));
    public static final StreamCodec<FriendlyByteBuf, StatelessBlockEntityStopAnimPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StatelessBlockEntityStopAnimPacket::blockPos,
            ByteBufCodecs.STRING_UTF8, StatelessBlockEntityStopAnimPacket::animation,
            StatelessBlockEntityStopAnimPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            if (ClientUtil.getLevel().getBlockEntity(this.blockPos) instanceof GeoBlockEntity blockEntity && blockEntity instanceof StatelessGeoBlockEntity statelessAnimatable)
                statelessAnimatable.handleClientAnimationStop(blockEntity, 0, this.animation);
        });
    }
}
