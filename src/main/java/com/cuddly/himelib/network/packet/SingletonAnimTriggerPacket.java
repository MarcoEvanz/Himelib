package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoAnimatable;
import com.cuddly.himelib.util.GeckoLibUtil;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record SingletonAnimTriggerPacket(String syncableId, long instanceId, String controllerName, String animName) implements MultiloaderPacket {
    public static final CustomPacketPayload.Type<SingletonAnimTriggerPacket> TYPE = new Type<>(GeckoLibConstants.id("singleton_anim_trigger"));
    public static final StreamCodec<FriendlyByteBuf, SingletonAnimTriggerPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SingletonAnimTriggerPacket::syncableId,
            ByteBufCodecs.VAR_LONG, SingletonAnimTriggerPacket::instanceId,
            ByteBufCodecs.STRING_UTF8, SingletonAnimTriggerPacket::controllerName,
            ByteBufCodecs.STRING_UTF8, SingletonAnimTriggerPacket::animName,
            SingletonAnimTriggerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            GeoAnimatable animatable = GeckoLibUtil.getSyncedAnimatable(this.syncableId);

            if (animatable != null)
                animatable.getAnimatableInstanceCache().getManagerForId(this.instanceId).tryTriggerAnimation(this.controllerName, this.animName);
        });
    }
}
