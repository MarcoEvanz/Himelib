package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoEntity;
import com.cuddly.himelib.animatable.GeoReplacedEntity;
import com.cuddly.himelib.constant.dataticket.SerializableDataTicket;
import com.cuddly.himelib.util.ClientUtil;
import com.cuddly.himelib.util.RenderUtil;

import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public record EntityDataSyncPacket<D>(int entityId, boolean isReplacedEntity, SerializableDataTicket<D> dataTicket, D data) implements MultiloaderPacket {
    public static final CustomPacketPayload.Type<EntityDataSyncPacket<?>> TYPE = new Type<>(GeckoLibConstants.id("entity_data_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityDataSyncPacket<?>> CODEC = StreamCodec.of((buf, packet) -> {
        SerializableDataTicket.STREAM_CODEC.encode(buf, packet.dataTicket);
        buf.writeVarInt(packet.entityId);
        buf.writeBoolean(packet.isReplacedEntity);
        ((StreamCodec)packet.dataTicket.streamCodec()).encode(buf, packet.data);
    }, buf -> {
        final SerializableDataTicket dataTicket = SerializableDataTicket.STREAM_CODEC.decode(buf);

        return new EntityDataSyncPacket<>(buf.readVarInt(), buf.readBoolean(), dataTicket, dataTicket.streamCodec().decode(buf));
    });

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

            if (!this.isReplacedEntity) {
                if (entity instanceof GeoEntity geoEntity)
                    geoEntity.setAnimData(this.dataTicket, this.data);

                return;
            }

            if (RenderUtil.getReplacedAnimatable(entity.getType()) instanceof GeoReplacedEntity replacedEntity)
                replacedEntity.setAnimData(entity, this.dataTicket, this.data);
        });
    }
}
