package engine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.file.Files;

public final class Utils {

//    private static PakFile pakFile;
//
//    static {
//        try {
//            pakFile = new PakFile(Utils.class.getResourceAsStream("/game/assets.pak"));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

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
