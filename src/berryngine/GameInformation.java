package berryngine;

import java.io.File;
import java.net.URISyntaxException;

public final class GameInformation {

    private static String name = "BerryNgineDemo";
    private static String author = "Unknown Author";
    private static String license = "All Rights Reserved";
    private static File gameDataFolder;
    private static File gameInstallFolder;
    private static String credits = "No credits provided.";

    static {
        gameInstallFolder = getJarDirectory();
        gameDataFolder = buildDefaultGameDataFolder(author, name);
    }

    // ONE static setter (your requirement)
    public static void set(String name,
                           String author,
                           String license,
                           String dataFolder,
                           String installFolder,
                           String credits) {

        GameInformation.name = name;
        GameInformation.author = author;
        GameInformation.license = license;
        GameInformation.credits = credits;

        gameInstallFolder = (installFolder == null || installFolder.trim().isEmpty())
                ? getJarDirectory()
                : new File(installFolder);

        gameDataFolder = (dataFolder == null || dataFolder.trim().isEmpty())
                ? buildDefaultGameDataFolder(author, name)
                : new File(dataFolder);

        printInformation();
    }

    private static File getJarDirectory() {
        try {
            File jarFile = new File(
                    GameInformation.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );
            return new File(jarFile.getParentFile().getAbsolutePath());
        } catch (URISyntaxException e) {
            return new File(new File(".").getAbsolutePath());
        }
    }

    private static File buildDefaultGameDataFolder(String author, String gameName) {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            String base = (appData != null && !appData.trim().isEmpty())
                    ? appData
                    : home + "\\AppData\\Roaming";

            return new File(base + "\\" + author + "\\" + gameName);
        }

        String xdg = System.getenv("XDG_DATA_HOME");
        String base = (xdg != null && !xdg.trim().isEmpty())
                ? xdg
                : home + "/.local/share";

        return new File(base + "/" + author + "/" + gameName);
    }

    public static void printInformation() {
        System.out.println("=== Game Information ===");
        System.out.println("Name: " + name);
        System.out.println("Author: " + author);
        System.out.println("License: " + license);
        System.out.println("Credits: " + credits);
        System.out.println("Install Folder: " + gameInstallFolder);
        System.out.println("Data Folder: " + gameDataFolder);
        System.out.println("========================");
    }

    // getters
    public static String getName() {
        return name;
    }

    public static String getAuthor() {
        return author;
    }

    public static String getLicense() {
        return license;
    }

    public static File getGameDataFolder() {
        return gameDataFolder;
    }

    public static File getGameInstallFolder() {
        return gameInstallFolder;
    }

    public static String getCredits() {
        return credits;
    }

    private GameInformation() {
    } // prevent instantiation
}