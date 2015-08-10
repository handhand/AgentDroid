package com.handhandlab.agentdroid.cert;

import java.nio.ByteOrder;

/**
 * change bytes order when generating X509 name hash
 *
 * Created by Handhand on 2015/6/1.
 */
public class Memory {
    public static int peekInt(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return (((src[offset++] & 0xff) << 24) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset++] & 0xff) << 8) |
                    ((src[offset ] & 0xff) << 0));
        } else {
            return (((src[offset++] & 0xff) << 0) |
                    ((src[offset++] & 0xff) << 8) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset ] & 0xff) << 24));
        }
    }
}
