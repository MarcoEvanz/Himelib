package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoBlockEntity;
import com.cuddly.himelib.animatable.stateless.StatelessGeoBlockEntity;
import com.cuddly.himelib.animation.RawAnimation;
import com.cuddly.himelib.network.packet.MultiloaderPacket;
import com.cuddly.himelib.util.ClientUtil;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record StatelessBlockEntityPlayAnimPacket(BlockPos blockPos, RawAnimation animation) implements MultiloaderPacket {
    public static final Type<StatelessBlockEntityPlayAnimPacket> TYPE = new Type<>(GeckoLibConstants.id("stateless_block_entity_play_anim"));
    public static final StreamCodec<FriendlyByteBuf, StatelessBlockEntityPlayAnimPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StatelessBlockEntityPlayAnimPacket::blockPos,
            RawAnimation.STREAM_CODEC, StatelessBlockEntityPlayAnimPacket::animation,
            StatelessBlockEntityPlayAnimPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            if (ClientUtil.getLevel().getBlockEntity(this.blockPos) instanceof GeoBlockEntity blockEntity && blockEntity instanceof StatelessGeoBlockEntity statelessAnimatable)
                statelessAnimatable.handleClientAnimationPlay(blockEntity, 0, this.animation);
        });
    }
}
