package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoAnimatable;
import com.cuddly.himelib.animatable.stateless.StatelessAnimatable;
import com.cuddly.himelib.network.packet.MultiloaderPacket;
import com.cuddly.himelib.util.ClientUtil;
import com.cuddly.himelib.util.RenderUtil;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public record StatelessEntityStopAnimPacket(int entityId, boolean isReplacedEntity, String animation) implements MultiloaderPacket {
    public static final Type<StatelessEntityStopAnimPacket> TYPE = new Type<>(GeckoLibConstants.id("stateless_entity_stop_anim"));
    public static final StreamCodec<FriendlyByteBuf, StatelessEntityStopAnimPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StatelessEntityStopAnimPacket::entityId,
            ByteBufCodecs.BOOL, StatelessEntityStopAnimPacket::isReplacedEntity,
            ByteBufCodecs.STRING_UTF8, StatelessEntityStopAnimPacket::animation,
            StatelessEntityStopAnimPacket::new);

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
                statelessAnimatable.handleClientAnimationStop(animatable, this.entityId, this.animation);
        });
    }
}
