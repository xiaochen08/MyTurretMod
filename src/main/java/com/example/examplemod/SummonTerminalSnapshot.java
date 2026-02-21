package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record SummonTerminalSnapshot(
        BlockPos terminalPos,
        int page,
        int totalPages,
        int totalCount,
        List<SummonTerminalEntry> entries
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(terminalPos);
        buf.writeVarInt(page);
        buf.writeVarInt(totalPages);
        buf.writeVarInt(totalCount);
        buf.writeVarInt(entries.size());
        for (SummonTerminalEntry entry : entries) {
            entry.encode(buf);
        }
    }

    public static SummonTerminalSnapshot decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int page = buf.readVarInt();
        int totalPages = buf.readVarInt();
        int totalCount = buf.readVarInt();
        int size = buf.readVarInt();
        List<SummonTerminalEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(SummonTerminalEntry.decode(buf));
        }
        return new SummonTerminalSnapshot(pos, page, totalPages, totalCount, entries);
    }
}
