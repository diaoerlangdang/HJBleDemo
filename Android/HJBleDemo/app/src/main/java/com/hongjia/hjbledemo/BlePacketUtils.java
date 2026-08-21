package com.hongjia.hjbledemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BlePacketUtils {
    private BlePacketUtils() {
    }

    public static List<byte[]> split(byte[] data, int packetLength) {
        if (packetLength <= 0) throw new IllegalArgumentException("packetLength must be positive");
        List<byte[]> packets = new ArrayList<>();
        for (int offset = 0; offset < data.length; offset += packetLength) {
            packets.add(Arrays.copyOfRange(data, offset, Math.min(offset + packetLength, data.length)));
        }
        return packets;
    }
}
