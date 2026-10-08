package vorez.mods.skins.impl.Utils;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import vorez.mods.skins.impl.SkinData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageUtils {

    private static final Logger LOG = LogUtils.getLogger();

    public static String judgeSkinType(byte[] data) {
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(data))) {
            int w = image.getWidth();
            int h = image.getHeight();

            if (w == h * 2)
                return "default";

            if (w == h) {
                int r = Math.max(w / 64, 1);
                if (((image.getPixel(55 * r, 20 * r) & 0xFF000000) >>> 24) == 0)
                    return "slim";
                return "default";
            }

            return "unknown";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    public static ByteBuffer legacySkinFilter(ByteBuffer buffer) {
        try {
            ByteBuffer readBuffer = buffer.asReadOnlyBuffer();
            readBuffer.rewind();

            try (NativeImage input = NativeImage.read(readBuffer)) {
                int width = input.getWidth();
                int height = input.getHeight();

                if (width == height && width >= 64 && width % 64 == 0) {
                    return imageToBuffer(input);
                }

                try (NativeImage output = new NativeImage(width, width, true)) {
                    int r = Math.max(width / 64, 1);
                    boolean f = width == height * 2;

                    output.copyFrom(input);

                    if (f) {
                        output.fillRect(0, 32 * r, 64 * r, 32 * r, 0);
                        output.copyRect(4 * r, 16 * r, 16 * r, 32 * r, 4 * r, 4 * r, true, false);
                        output.copyRect(8 * r, 16 * r, 16 * r, 32 * r, 4 * r, 4 * r, true, false);
                        output.copyRect(0, 20 * r, 24 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(4 * r, 20 * r, 16 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(8 * r, 20 * r, 8 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(12 * r, 20 * r, 16 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(44 * r, 16 * r, -8 * r, 32 * r, 4 * r, 4 * r, true, false);
                        output.copyRect(48 * r, 16 * r, -8 * r, 32 * r, 4 * r, 4 * r, true, false);
                        output.copyRect(40 * r, 20 * r, 0, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(44 * r, 20 * r, -8 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(48 * r, 20 * r, -16 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(52 * r, 20 * r, -8 * r, 32 * r, 4 * r, 12 * r, true, false);
                    }

                    setAreaOpaque(output, 0, 0, 32 * r, 16 * r);

                    if (f)
                        setAreaTransparent(output, 32 * r, 0, 64 * r, 32 * r);

                    setAreaOpaque(output, 0, 16 * r, 64 * r, 32 * r);
                    setAreaOpaque(output, 16 * r, 48 * r, 48 * r, 64 * r);

                    return imageToBuffer(output);
                }
            }
        } catch (Throwable t) {
            LOG.error("[OfflineSkins-Reloaded] Failed to filter skin texture.", t);
            buffer.rewind();
            return buffer;
        }
    }

    public static ByteBuffer legacyCapeFilter(ByteBuffer buffer) {
        try {
            ByteBuffer readBuffer = buffer.asReadOnlyBuffer();
            readBuffer.rewind();

            try (NativeImage input = NativeImage.read(readBuffer)) {
                int width = input.getWidth();
                int height = input.getHeight();

                if (width >= 64 && width % 64 == 0 && height == width / 2) {
                    return imageToBuffer(input);
                }

                if (width >= 22 && width % 22 == 0 && height % 17 == 0) {
                    int scale = width / 22;
                    if (height / 17 == scale && (scale & (scale - 1)) == 0) {

                        int targetWidth = 64 * scale;
                        int targetHeight = 32 * scale;

                        try (NativeImage output = new NativeImage(
                                targetWidth,
                                targetHeight,
                                true
                        )) {
                            output.fillRect(
                                    0,
                                    0,
                                    targetWidth,
                                    targetHeight,
                                    0
                            );

                            for (int x = 0; x < width; x++)
                                for (int y = 0; y < height; y++)
                                    output.setPixel(
                                            x,
                                            y,
                                            input.getPixel(x, y)
                                    );

                            return imageToBuffer(output);
                        }
                    }
                }

                buffer.rewind();
                return buffer;
            }
        } catch (Throwable t) {
            LOG.error("[OfflineSkins-Reloaded] Failed to filter cape texture.", t);
            buffer.rewind();
            return buffer;
        }
    }

    private static ByteBuffer imageToBuffer(NativeImage image) throws IOException {
        Path path = Files.createTempFile(null, null);
        try {
            image.writeToFile(path);
            return SkinData.toBuffer(Files.readAllBytes(path));
        } finally {
            File file = path.toFile();
            if (file.exists() && !file.delete())
                file.deleteOnExit();
        }
    }

    private static void setAreaOpaque(NativeImage image, int x, int y, int width, int height) {
        int endX = Math.min(width, image.getWidth());
        int endY = Math.min(height, image.getHeight());

        for (int i = x; i < endX; ++i)
            for (int j = y; j < endY; ++j)
                image.setPixel(i, j, image.getPixel(i, j) | 0xFF000000);
    }

    private static void setAreaTransparent(NativeImage image, int x, int y, int width, int height) {
        int endX = Math.min(width, image.getWidth());
        int endY = Math.min(height, image.getHeight());

        for (int i = x; i < endX; ++i)
            for (int j = y; j < endY; ++j)
                if (((image.getPixel(i, j) >>> 24) & 0xFF) < 128)
                    return;

        for (int x1 = x; x1 < endX; ++x1)
            for (int y1 = y; y1 < endY; ++y1)
                image.setPixel(x1, y1, image.getPixel(x1, y1) & 0x00FFFFFF);
    }

    public record ImageSize(int width, int height) {
    }

    public static ImageSize getImageSize(byte[] data) {
        if (data == null || data.length < 24)
            return null;

        if (data[0] != (byte) 0x89
                || data[1] != 0x50
                || data[2] != 0x4E
                || data[3] != 0x47
                || data[4] != 0x0D
                || data[5] != 0x0A
                || data[6] != 0x1A
                || data[7] != 0x0A)
            return null;

        if (data[12] != 0x49
                || data[13] != 0x48
                || data[14] != 0x44
                || data[15] != 0x52)
            return null;

        int width = ((data[16] & 0xFF) << 24)
                | ((data[17] & 0xFF) << 16)
                | ((data[18] & 0xFF) << 8)
                | (data[19] & 0xFF);

        int height = ((data[20] & 0xFF) << 24)
                | ((data[21] & 0xFF) << 16)
                | ((data[22] & 0xFF) << 8)
                | (data[23] & 0xFF);

        if (width <= 0 || height <= 0)
            return null;

        return new ImageSize(width, height);
    }

    public static boolean isSkinResolutionAllowed(int width, int height, boolean allowHd, int maxHDResolution) {
        if (!allowHd)
            return width == 64 && (height == 32 || height == 64);

        if (width < 64 || height < 32)
            return false;

        if (width % 64 != 0)
            return false;

        int scale = width / 64;

        if (height != 32 * scale && height != 64 * scale)
            return false;

        return maxHDResolution == -1 || (width <= maxHDResolution && height <= maxHDResolution);
    }

    public static boolean isCapeResolutionAllowed(int width, int height, boolean allowHd, int maxHDResolution) {
        if (width == 22 && height == 17)
            return true;

        if (!allowHd)
            return width == 64 && height == 32;

        if (width >= 64 && width % 64 == 0 && height == width / 2) {
            return maxHDResolution == -1 || (width <= maxHDResolution && height <= maxHDResolution);
        }

        if (width >= 22 && width % 22 == 0 && height % 17 == 0) {
            int scale = width / 22;

            if (height / 17 != scale || (scale & (scale - 1)) != 0)
                return false;

            int targetWidth = 64 * scale;
            int targetHeight = 32 * scale;

            return maxHDResolution == -1 || (targetWidth <= maxHDResolution && targetHeight <= maxHDResolution);
        }

        return false;
    }

    public static boolean validateData(byte[] data) {
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(data))) {
            return image != null;
        } catch (Throwable t) {
            return false;
        }
    }
}