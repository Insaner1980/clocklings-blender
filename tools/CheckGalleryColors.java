import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/** Compares the two 411 dp cells pulled from VisualFollowUpRound2CaptureTest. */
class CheckGalleryColors {
    private static final String SPLASH = "startup--01-android-splash.png";
    private static final int LIGHT_SPLASH = 0xFFF5FA;
    private static final int BANNER_BOTTOM_DP = 168;
    private static final int RGB_TOLERANCE = 4;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java tools/CheckGalleryColors.java <gallery-directory>");
        }
        ImageIO.setUseCache(false);
        Path gallery = Path.of(args[0]);
        Path light = gallery.resolve("411dp-100");
        Path dark = gallery.resolve("411dp-100-dark");
        SortedSet<String> lightNames = appScreenshots(light);
        SortedSet<String> darkNames = appScreenshots(dark);
        if (!lightNames.equals(darkNames)) {
            var missingLight = new TreeSet<>(darkNames);
            missingLight.removeAll(lightNames);
            var missingDark = new TreeSet<>(lightNames);
            missingDark.removeAll(darkNames);
            throw new IllegalStateException("Unpaired app screenshots: missing in light " + missingLight
                + ", missing in expanded dark " + missingDark);
        }
        Map<String, Integer> lightInsets = readInsets(light);
        Map<String, Integer> darkInsets = readInsets(dark);
        int lightSplash = splashColor(light);
        System.out.printf("Android splash: light #%06X, expanded dark #%06X%n", lightSplash, splashColor(dark));
        if (lightSplash != LIGHT_SPLASH) {
            throw new IllegalStateException("The light launch must have the Clocklings splash background");
        }

        int failed = 0;
        for (String name : lightNames) {
            int bottom = Math.max(requireInset(lightInsets, name), requireInset(darkInsets, name));
            int different = differentPixels(readImage(light.resolve(name)), readImage(dark.resolve(name)), bottom);
            if (different != 0) {
                System.err.printf("%s: %d pixels differ beyond the RGB tolerance%n", name, different);
                failed++;
            }
        }
        if (lightNames.isEmpty() || failed != 0) {
            throw new IllegalStateException("Compared " + lightNames.size() + " app screenshots, " + failed + " failed");
        }
        System.out.printf("Compared %d app screenshots within RGB tolerance %d%n", lightNames.size(), RGB_TOLERANCE);
    }

    private static SortedSet<String> appScreenshots(Path cell) throws IOException {
        try (var files = Files.list(cell)) {
            return files.filter(Files::isRegularFile)
                .map(file -> file.getFileName().toString())
                .filter(name -> name.endsWith(".png") && !name.equals(SPLASH))
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Map<String, Integer> readInsets(Path cell) throws IOException {
        var insets = new HashMap<String, Integer>();
        for (String line : Files.readAllLines(cell.resolve("app-area.tsv"))) {
            String[] fields = line.split("\t", 2);
            insets.put(fields[0], Integer.parseInt(fields[1]));
        }
        return insets;
    }

    private static int requireInset(Map<String, Integer> insets, String name) {
        Integer bottom = insets.get(name);
        if (bottom == null || bottom < 0) throw new IllegalStateException("Missing or invalid app area for " + name);
        return bottom;
    }

    private static BufferedImage readImage(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) throw new IOException("Could not read " + path);
        return image;
    }

    private static int splashColor(Path cell) throws IOException {
        var image = readImage(cell.resolve(SPLASH));
        return image.getRGB(image.getWidth() / 4, image.getHeight() / 4) & 0xFFFFFF;
    }

    private static int differentPixels(BufferedImage light, BufferedImage dark, int bottomInset) {
        int width = light.getWidth();
        int height = light.getHeight();
        if (width != dark.getWidth() || height != dark.getHeight()) {
            throw new IllegalArgumentException("Gallery image sizes differ");
        }
        // Start below the app header and notification banner area. System navigation and the IME are below the crop.
        int top = (int) Math.ceil(width * BANNER_BOTTOM_DP / 411.0);
        int bottom = height - bottomInset;
        if (top >= bottom) throw new IllegalArgumentException("The app area is empty");
        int[] lightRow = new int[width];
        int[] darkRow = new int[width];
        int different = 0;
        for (int y = top; y < bottom; y++) {
            light.getRGB(0, y, width, 1, lightRow, 0, width);
            dark.getRGB(0, y, width, 1, darkRow, 0, width);
            for (int x = 0; x < width; x++) {
                int a = lightRow[x];
                int b = darkRow[x];
                if (channelDiff(a, b, 16) || channelDiff(a, b, 8) || channelDiff(a, b, 0)) different++;
            }
        }
        return different;
    }

    private static boolean channelDiff(int a, int b, int shift) {
        return Math.abs(((a >> shift) & 0xFF) - ((b >> shift) & 0xFF)) > RGB_TOLERANCE;
    }
}
