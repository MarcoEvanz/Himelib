package com.cuddly.himelib.network.packet;

import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animatable.GeoAnimatable;
import com.cuddly.himelib.animatable.SingletonGeoAnimatable;
import com.cuddly.himelib.constant.dataticket.SerializableDataTicket;
import com.cuddly.himelib.util.ClientUtil;
import com.cuddly.himelib.util.GeckoLibUtil;

import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record SingletonDataSyncPacket<D>(String syncableId, long instanceId, SerializableDataTicket<D> dataTicket, D data) implements MultiloaderPacket {
    public static final CustomPacketPayload.Type<SingletonDataSyncPacket<?>> TYPE = new Type<>(GeckoLibConstants.id("singleton_data_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SingletonDataSyncPacket<?>> CODEC = StreamCodec.of((buf, packet) -> {
        SerializableDataTicket.STREAM_CODEC.encode(buf, packet.dataTicket);
        buf.writeUtf(packet.syncableId);
        buf.writeVarLong(packet.instanceId);
        ((StreamCodec)packet.dataTicket.streamCodec()).encode(buf, packet.data);
    }, buf -> {
        final SerializableDataTicket dataTicket = SerializableDataTicket.STREAM_CODEC.decode(buf);

        return new SingletonDataSyncPacket<>(buf.readUtf(), buf.readVarLong(), dataTicket, dataTicket.streamCodec().decode(buf));
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            GeoAnimatable animatable = GeckoLibUtil.getSyncedAnimatable(this.syncableId);

            if (animatable instanceof SingletonGeoAnimatable singleton)
                singleton.setAnimData(ClientUtil.getClientPlayer(), this.instanceId, this.dataTicket, this.data);
        });
    }
}
