package wiki.creeper.creeperPrefixSystem.util;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Converts UUIDs to byte arrays compatible with BINARY(16) columns.
 */
public final class UuidUtil {
    private UuidUtil() {
    }

    public static byte[] toBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID fromBytes(byte[] data) {
        if (data == null || data.length != 16) {
            throw new IllegalArgumentException("Expected 16 byte array for UUID");
        }
        ByteBuffer bb = ByteBuffer.wrap(data);
        long most = bb.getLong();
        long least = bb.getLong();
        return new UUID(most, least);
    }
}
