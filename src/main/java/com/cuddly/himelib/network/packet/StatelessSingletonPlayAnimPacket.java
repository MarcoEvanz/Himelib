package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoAnimatable;
import com.cuddly.himelib.animatable.stateless.StatelessGeoSingletonAnimatable;
import com.cuddly.himelib.animation.RawAnimation;
import com.cuddly.himelib.util.GeckoLibUtil;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record StatelessSingletonPlayAnimPacket(String syncableId, long instanceId, RawAnimation animation) implements MultiloaderPacket {
    public static final Type<StatelessSingletonPlayAnimPacket> TYPE = new Type<>(GeckoLibConstants.id("stateless_singleton_play_anim"));
    public static final StreamCodec<FriendlyByteBuf, StatelessSingletonPlayAnimPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StatelessSingletonPlayAnimPacket::syncableId,
            ByteBufCodecs.VAR_LONG, StatelessSingletonPlayAnimPacket::instanceId,
            RawAnimation.STREAM_CODEC, StatelessSingletonPlayAnimPacket::animation,
            StatelessSingletonPlayAnimPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            GeoAnimatable animatable = GeckoLibUtil.getSyncedAnimatable(this.syncableId);

            if (animatable instanceof StatelessGeoSingletonAnimatable statelessAnimatable)
                statelessAnimatable.handleClientAnimationPlay(animatable, this.instanceId, this.animation);
        });
    }
}
