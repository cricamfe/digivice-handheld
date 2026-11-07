package dev.cricamfe.digivice.setup;

import java.io.FileDescriptor;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Host parity check using the same JNI reader as Android. */
public final class ChdNative {
    static { System.loadLibrary("digivice_chd"); }

    private native long open(int fd);
    private native long size(long handle);
    private native boolean read(long handle, long position, byte[] output, int offset, int length);
    private native void close(long handle);

    private static int fd(RandomAccessFile file) throws ReflectiveOperationException, java.io.IOException {
        var field = FileDescriptor.class.getDeclaredField("fd");
        field.setAccessible(true);
        return field.getInt(file.getFD());
    }

    public static void main(String[] args) throws Exception {
        var nativeReader = new ChdNative();
        var truncated = Files.createTempFile("chd-header", ".chd");
        try (var image = new RandomAccessFile(args[0], "r");
             var bin = new RandomAccessFile(args[1], "r")) {
            var header = new byte[124];
            image.readFully(header);
            Files.write(truncated, header);
            try (var invalid = new RandomAccessFile(truncated.toFile(), "r")) {
                if (nativeReader.open(fd(invalid)) != 0) throw new AssertionError("Accepted a truncated CHD");
            }

            long handle = nativeReader.open(fd(image));
            if (handle == 0) throw new AssertionError("Could not open the CHD");
            try {
                long sectors = bin.length() / 2352;
                if (nativeReader.size(handle) != bin.length()) throw new AssertionError("Disc size differs");
                for (int sample = 0; sample < 1000; sample++) {
                    long sector = sample * (sectors - 1) / 999;
                    nativeReader.compare(handle, bin, sector * 2352, 2352);
                }
                var framesPerHunk = java.nio.ByteBuffer.wrap(header).getInt(56) / 2448;
                nativeReader.compare(handle, bin, framesPerHunk * 2352L - 1, 4);
            } finally {
                nativeReader.close(handle);
            }
        } finally {
            Files.deleteIfExists(truncated);
        }
        System.out.println("1000 raw sectors and a read spanning two hunks match the BIN. Truncated CHD rejected.");
    }

    private void compare(long handle, RandomAccessFile bin, long position, int length) throws java.io.IOException {
        var expected = new byte[length];
        bin.seek(position);
        bin.readFully(expected);
        var actual = new byte[length + 2];
        actual[0] = actual[length + 1] = 42;
        if (!read(handle, position, actual, 1, length) ||
            actual[0] != 42 || actual[length + 1] != 42 ||
            !Arrays.equals(expected, 0, length, actual, 1, length + 1)) {
            throw new AssertionError("Read differs at byte " + position);
        }
    }
}
