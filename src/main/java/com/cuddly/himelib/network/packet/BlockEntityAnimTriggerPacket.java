package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoBlockEntity;
import com.cuddly.himelib.util.ClientUtil;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record BlockEntityAnimTriggerPacket(BlockPos pos, String controllerName, String animName) implements MultiloaderPacket {
    public static final CustomPacketPayload.Type<BlockEntityAnimTriggerPacket> TYPE = new Type<>(GeckoLibConstants.id("blockentity_anim_trigger"));
    public static final StreamCodec<FriendlyByteBuf, BlockEntityAnimTriggerPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlockEntityAnimTriggerPacket::pos,
            ByteBufCodecs.STRING_UTF8, BlockEntityAnimTriggerPacket::controllerName,
            ByteBufCodecs.STRING_UTF8, BlockEntityAnimTriggerPacket::animName,
            BlockEntityAnimTriggerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            if (ClientUtil.getLevel().getBlockEntity(this.pos) instanceof GeoBlockEntity blockEntity)
                blockEntity.triggerAnim(this.controllerName.isEmpty() ? null : this.controllerName, this.animName);
        });
    }
}
