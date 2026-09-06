package berryngine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class Utils {

    public static byte[] getFileFromResources(String fileName) {
        try (InputStream is = Utils.class.getResourceAsStream(fileName)) {

            if (is == null) {
                throw new IllegalArgumentException("File not found: " + fileName);
            }

            return toByteArray(is);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] getFileFromGameInstall(String filePath) {
        try (InputStream is = Files.newInputStream(new File(GameInformation.getGameInstallFolder(), filePath).toPath())) {
            return toByteArray(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] getFileFromGameInstallZip(String zipPath, String entryPath) {
        return getFileFromZip(new File(GameInformation.getGameInstallFolder(), zipPath), entryPath);
    }

    private static byte[] getFileFromZip(File zipFile, String entryPath) {
        if (entryPath == null) {
            throw new IllegalArgumentException("entryPath cannot be null");
        }

        String normalized = entryPath.startsWith("/") ? entryPath.substring(1) : entryPath;

        try (ZipFile zf = new ZipFile(zipFile)) {
            ZipEntry entry = zf.getEntry(normalized);
            if (entry == null) {
                throw new IllegalArgumentException("Entry not found in zip: " + entryPath + " (zip: " + zipFile + ")");
            }

            try (InputStream is = zf.getInputStream(entry)) {
                return toByteArray(is);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] getFileFromResourcesZip(String resourcePath, String entryPath) {
        InputStream zipStream = Utils.class.getResourceAsStream(resourcePath);
        if (zipStream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return getFileFromZip(zipStream, entryPath);
    }

    private static byte[] getFileFromZip(InputStream zipStream, String entryPath) {
        if (entryPath == null) {
            throw new IllegalArgumentException("entryPath cannot be null");
        }

        String normalized = entryPath.startsWith("/") ? entryPath.substring(1) : entryPath;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zipStream))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(normalized)) {
                    return toByteArray(zis);
                }
            }
            throw new IllegalArgumentException("Entry not found in zip: " + entryPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean hasExtension(String fileName, String extension) {
        if (fileName == null || extension == null) {
            return false;
        }

        return fileName.toLowerCase().endsWith("." + extension.toLowerCase());
    }

    public static Sound loadSoundFromGameInstall(String relativePath) {
        try {
            return QOADecoder.decode(getFileFromGameInstall(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Sound loadSoundFromResources(String resourcePath) {
        try {
            return QOADecoder.decode(getFileFromResources(resourcePath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static PixelGraphics loadTextureFromGameInstall(String relativePath) {
        try {
            if (hasExtension(relativePath, "qoi") || hasExtension(relativePath, "qoif")) {
                return loadTextureFromQOI(getFileFromGameInstall(relativePath));
            }

            BufferedImage img = ImageIO.read(
                    new ByteArrayInputStream(getFileFromGameInstall(relativePath))
            );

            return textureFromBufferedImage(img);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static PixelGraphics loadTextureFromResources(String resourcePath) {
        if (hasExtension(resourcePath, "qoi") || hasExtension(resourcePath, "qoif")) {
            return loadTextureFromQOI(getFileFromResources(resourcePath));
        }

        try {
            return textureFromBufferedImage(ImageIO.read(new ByteArrayInputStream(getFileFromResources(resourcePath))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TextureAtlas loadTextureAtlasFromGameInstall(String relativePath, int tileWidth, int tileHeight) {
        PixelGraphics texture = loadTextureFromGameInstall(relativePath);
        return new TextureAtlas(texture, tileWidth, tileHeight);
    }

    public static TextureAtlas loadTextureAtlasFromResources(String resourcePath, int tileWidth, int tileHeight) {
        PixelGraphics texture = loadTextureFromResources(resourcePath);
        return new TextureAtlas(texture, tileWidth, tileHeight);
    }

    public static Sound loadSoundFromGameInstallZip(String zipPath, String entryPath) {
        try {
            return QOADecoder.decode(getFileFromGameInstallZip(zipPath, entryPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static PixelGraphics loadTextureFromGameInstallZip(String zipPath, String entryPath) {
        try {
            byte[] data = getFileFromGameInstallZip(zipPath, entryPath);

            if (hasExtension(entryPath, "qoi") || hasExtension(entryPath, "qoif")) {
                return loadTextureFromQOI(data);
            }

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            return textureFromBufferedImage(img);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TextureAtlas loadTextureAtlasFromGameInstallZip(String zipPath, String entryPath, int tileWidth, int tileHeight) {
        PixelGraphics texture = loadTextureFromGameInstallZip(zipPath, entryPath);
        return new TextureAtlas(texture, tileWidth, tileHeight);
    }

    public static MusicSynth loadMidiFromGameInstallZip(String zipPath, String entryPath) {
        return MidiImporter.load(getFileFromGameInstallZip(zipPath, entryPath));
    }

    public static BitmapFont loadFontFromGameInstallZip(String zipPath, String entryPath) {
        return PSF1Parser.parse(getFileFromGameInstallZip(zipPath, entryPath));
    }

    public static Sound loadSoundFromResourcesZip(String resourcePath, String entryPath) {
        try {
            return QOADecoder.decode(getFileFromResourcesZip(resourcePath, entryPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static PixelGraphics loadTextureFromResourcesZip(String resourcePath, String entryPath) {
        try {
            byte[] data = getFileFromResourcesZip(resourcePath, entryPath);

            if (hasExtension(entryPath, "qoi") || hasExtension(entryPath, "qoif")) {
                return loadTextureFromQOI(data);
            }

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            return textureFromBufferedImage(img);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TextureAtlas loadTextureAtlasFromResourcesZip(String resourcePath, String entryPath, int tileWidth, int tileHeight) {
        PixelGraphics texture = loadTextureFromResourcesZip(resourcePath, entryPath);
        return new TextureAtlas(texture, tileWidth, tileHeight);
    }

    public static MusicSynth loadMidiFromResourcesZip(String resourcePath, String entryPath) {
        return MidiImporter.load(getFileFromResourcesZip(resourcePath, entryPath));
    }

    public static BitmapFont loadFontFromResourcesZip(String resourcePath, String entryPath) {
        return PSF1Parser.parse(getFileFromResourcesZip(resourcePath, entryPath));
    }

    private static PixelGraphics loadTextureFromQOI(byte[] qoiFileData) {
        return QOIDecoder.decode(qoiFileData);
    }

    public static PixelGraphics textureFromBufferedImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Ensure we get an ARGB-compatible image (important for safety)
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            BufferedImage converted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            converted.getGraphics().drawImage(image, 0, 0, null);
            image = converted;
        }

        int[] pixels = ((DataBufferInt) image.getRaster()
                .getDataBuffer())
                .getData();

        // Copy so we don't accidentally alias Swing-managed memory
        int[] copy = new int[pixels.length];
        System.arraycopy(pixels, 0, copy, 0, pixels.length);

        return new PixelGraphics(copy, width, height);
    }

    public static MusicSynth loadMidiFromResources(String resourcePath) {
        return MidiImporter.load(getFileFromResources(resourcePath));
    }

    public static MusicSynth loadMidiFromFile(File file) {
        return MidiImporter.load(file);
    }

    public static MusicSynth loadMidiFromGameInstall(String relativePath) {
        return MidiImporter.load(new File(GameInformation.getGameInstallFolder(), relativePath));
    }

    public static BitmapFont loadFontFromGameInstall(String relativePath) {
        return PSF1Parser.parse(getFileFromGameInstall(relativePath));
    }

    public static BitmapFont loadFontFromResources(String resourcePath) {
        return PSF1Parser.parse(getFileFromResources(resourcePath));
    }

    public static void saveScreenshot(PixelGraphics g, String filePath) {
        try {
            java.io.File out = new java.io.File(filePath);
            if (out.getParentFile() != null) out.getParentFile().mkdirs();
            ImageIO.write(g.toBufferedImage(), "PNG", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void saveScreenshot(PixelGraphics g) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        saveScreenshot(g, "screenshot_" + timestamp + ".png");
    }

    private static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

}
