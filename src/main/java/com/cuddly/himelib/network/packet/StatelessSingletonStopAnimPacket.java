package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoAnimatable;
import com.cuddly.himelib.animatable.stateless.StatelessGeoSingletonAnimatable;
import com.cuddly.himelib.util.GeckoLibUtil;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record StatelessSingletonStopAnimPacket(String syncableId, long instanceId, String animation) implements MultiloaderPacket {
    public static final Type<StatelessSingletonStopAnimPacket> TYPE = new Type<>(GeckoLibConstants.id("stateless_singleton_stop_anim"));
    public static final StreamCodec<FriendlyByteBuf, StatelessSingletonStopAnimPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StatelessSingletonStopAnimPacket::syncableId,
            ByteBufCodecs.VAR_LONG, StatelessSingletonStopAnimPacket::instanceId,
            ByteBufCodecs.STRING_UTF8, StatelessSingletonStopAnimPacket::animation,
            StatelessSingletonStopAnimPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            GeoAnimatable animatable = GeckoLibUtil.getSyncedAnimatable(this.syncableId);

            if (animatable instanceof StatelessGeoSingletonAnimatable statelessAnimatable)
                statelessAnimatable.handleClientAnimationStop(animatable, this.instanceId, this.animation);
        });
    }
}
