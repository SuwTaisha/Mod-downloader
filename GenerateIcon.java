import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** One-off tool: resizes the user-supplied source image into a multi-size .ico for the app icon. */
public class GenerateIcon {

    public static void main(String[] args) throws IOException {
        String sourcePath = args.length > 0 ? args[0] : "icon.jpg";
        BufferedImage source = ImageIO.read(new File(sourcePath));
        if (source == null) {
            throw new IOException("Could not read image: " + sourcePath);
        }

        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        List<byte[]> pngFrames = new ArrayList<>();
        for (int size : sizes) {
            BufferedImage resized = resize(source, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", out);
            pngFrames.add(out.toByteArray());
        }
        writeIco("icon.ico", sizes, pngFrames);
        System.out.println("Wrote icon.ico with sizes: " + java.util.Arrays.toString(sizes));
    }

    private static BufferedImage resize(BufferedImage source, int size) {
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source, 0, 0, size, size, null);
        g2.dispose();
        return resized;
    }

    /** Minimal ICO writer using the "PNG-in-ICO" format (supported since Windows Vista) - no external deps needed. */
    private static void writeIco(String path, int[] sizes, List<byte[]> pngFrames) throws IOException {
        int count = sizes.length;
        int offset = 6 + count * 16;

        try (FileOutputStream fos = new FileOutputStream(path)) {
            writeU16(fos, 0);
            writeU16(fos, 1);
            writeU16(fos, count);

            for (int i = 0; i < count; i++) {
                int size = sizes[i];
                byte[] png = pngFrames.get(i);
                fos.write(size >= 256 ? 0 : size);
                fos.write(size >= 256 ? 0 : size);
                fos.write(0);
                fos.write(0);
                writeU16(fos, 1);
                writeU16(fos, 32);
                writeU32(fos, png.length);
                writeU32(fos, offset);
                offset += png.length;
            }
            for (byte[] png : pngFrames) {
                fos.write(png);
            }
        }
    }

    private static void writeU16(FileOutputStream fos, int value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
    }

    private static void writeU32(FileOutputStream fos, int value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 24) & 0xFF);
    }
}
