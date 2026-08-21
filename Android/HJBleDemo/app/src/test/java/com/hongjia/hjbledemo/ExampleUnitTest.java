package com.hongjia.hjbledemo;

import com.wise.ble.ConvertData;
import com.wise.ble.WiseWaitEvent;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExampleUnitTest {
    @Test
    public void splitPackets_preservesDataAndPacketLimit() {
        byte[] input = new byte[45];
        for (int i = 0; i < input.length; i++) input[i] = (byte) i;

        List<byte[]> packets = BlePacketUtils.split(input, 20);

        assertEquals(3, packets.size());
        assertEquals(20, packets.get(0).length);
        assertEquals(20, packets.get(1).length);
        assertEquals(5, packets.get(2).length);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] packet : packets) output.write(packet, 0, packet.length);
        assertArrayEquals(input, output.toByteArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitPackets_rejectsInvalidPacketLength() {
        BlePacketUtils.split(new byte[]{1}, 0);
    }

    @Test
    public void splitPackets_respectsNegotiatedPacketLength() {
        byte[] input = new byte[500];

        List<byte[]> packets = BlePacketUtils.split(input, 244);

        assertEquals(3, packets.size());
        assertEquals(244, packets.get(0).length);
        assertEquals(244, packets.get(1).length);
        assertEquals(12, packets.get(2).length);
    }

    @Test
    public void hexConversion_roundTrips() {
        byte[] bytes = ConvertData.hexStringToBytes("00A1ff");
        assertEquals("00a1ff", ConvertData.bytesToHexString(bytes, false));
    }

    @Test
    public void waitEvent_supportsSignalTimeoutAndInterruption() throws Exception {
        WiseWaitEvent signaled = new WiseWaitEvent();
        signaled.init();
        signaled.setSignal(WiseWaitEvent.SUCCESS);
        assertEquals(WiseWaitEvent.SUCCESS, signaled.waitSignal(50));

        WiseWaitEvent timedOut = new WiseWaitEvent();
        timedOut.init();
        assertEquals(WiseWaitEvent.ERROR_TIME_OUT, timedOut.waitSignal(5));

        WiseWaitEvent interrupted = new WiseWaitEvent();
        interrupted.init();
        AtomicInteger interruptedResult = new AtomicInteger();
        Thread thread = new Thread(() -> interruptedResult.set(interrupted.waitSignal(5000)));
        thread.start();
        thread.interrupt();
        thread.join(1000);
        assertTrue(!thread.isAlive());
        assertEquals(WiseWaitEvent.ERROR_FAILED, interruptedResult.get());
    }

    @Test
    public void waitEvents_keepSignalsIsolated() {
        WiseWaitEvent firstAttempt = new WiseWaitEvent();
        WiseWaitEvent secondAttempt = new WiseWaitEvent();
        firstAttempt.init();
        secondAttempt.init();

        firstAttempt.setSignal(WiseWaitEvent.SUCCESS);

        assertEquals(WiseWaitEvent.SUCCESS, firstAttempt.waitSignal(5));
        assertEquals(WiseWaitEvent.ERROR_TIME_OUT, secondAttempt.waitSignal(5));
    }
}
