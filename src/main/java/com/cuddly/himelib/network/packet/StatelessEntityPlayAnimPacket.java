package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoAnimatable;
import com.cuddly.himelib.animatable.stateless.StatelessAnimatable;
import com.cuddly.himelib.animation.RawAnimation;
import com.cuddly.himelib.util.ClientUtil;
import com.cuddly.himelib.util.RenderUtil;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public record StatelessEntityPlayAnimPacket(int entityId, boolean isReplacedEntity, RawAnimation animation) implements MultiloaderPacket {
    public static final Type<StatelessEntityPlayAnimPacket> TYPE = new Type<>(GeckoLibConstants.id("stateless_entity_play_anim"));
    public static final StreamCodec<FriendlyByteBuf, StatelessEntityPlayAnimPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StatelessEntityPlayAnimPacket::entityId,
            ByteBufCodecs.BOOL, StatelessEntityPlayAnimPacket::isReplacedEntity,
            RawAnimation.STREAM_CODEC, StatelessEntityPlayAnimPacket::animation,
            StatelessEntityPlayAnimPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            Entity entity = ClientUtil.getLevel().getEntity(this.entityId);

            if (entity == null)
                return;

            GeoAnimatable animatable = this.isReplacedEntity ?
                                       RenderUtil.getReplacedAnimatable(entity.getType()) :
                                       entity instanceof GeoAnimatable entityAnimatable ? entityAnimatable : null;

            if (animatable instanceof StatelessAnimatable statelessAnimatable)
                statelessAnimatable.handleClientAnimationPlay(animatable, this.entityId, this.animation);
        });
    }
}
