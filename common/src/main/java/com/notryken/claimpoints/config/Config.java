package com.notryken.claimpoints.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.notryken.claimpoints.ClaimPoints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Config {
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = ClaimPoints.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options

    public final ClaimPointSettings cpSettings = new ClaimPointSettings();
    public final GriefPreventionSettings gpSettings = new GriefPreventionSettings();

    public static class ClaimPointSettings {
        public static final String defaultNameFormat = "CP (%d)";
        public String nameFormat = defaultNameFormat;

        public static final String defaultNamePattern = "^CP \\((\\d+)\\)$";
        public String namePattern = defaultNamePattern;
        public transient Pattern nameCompiled;

        public static final String defaultAlias = "C";
        public String alias = defaultAlias;

        public static final String defaultColor = ClaimPoints.waypointColorNames.getLast();
        public String color = defaultColor;
        public transient int colorIdx;
    }

    public static class GriefPreventionSettings {
        public static final String defaultFirstLinePattern =
                "^-?\\d+ blocks from play \\+ -?\\d+ bonus = -?\\d+ total.$";
        public String firstLinePattern = defaultFirstLinePattern;
        public transient Pattern firstLineCompiled;

        public static final String defaultClaimLinePattern =
                "^(.+): x(-?\\d+), z(-?\\d+) \\(-?(\\d+) blocks\\)$";
        public String claimLinePattern = defaultClaimLinePattern;
        public transient Pattern claimLineCompiled;

        public static final List<String> defaultIgnoredLinePatterns = List.of(
                "^Claims:$"
        );
        public List<String> ignoredLinePatterns = new ArrayList<>(defaultIgnoredLinePatterns);
        public transient List<Pattern> ignoredLinesCompiled;

        public static final List<String> defaultEndingLinePatterns = List.of(
                "^ = -?\\d* blocks left to spend$"
        );
        public List<String> endingLinePatterns = new ArrayList<>(defaultEndingLinePatterns);
        public transient List<Pattern> endingLinesCompiled;
    }


    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
            instance.verifyConfig();
        }
        return instance;
    }

    public static Config getAndSave() {
        get().verifyConfig();
        save();
        return instance;
    }

    public static Config resetAndSave() {
        instance = new Config();
        instance.verifyConfig();
        save();
        return instance;
    }

    // Verification

    public void verifyConfig() {
        int indexOfSize = cpSettings.nameFormat.indexOf("%d");
        if (indexOfSize == -1) {
            throw new IllegalArgumentException("Name format '" + cpSettings.nameFormat +
                    "' missing required sequence %d.");
        }
        else {
            cpSettings.namePattern = "^" + Pattern.quote(cpSettings.nameFormat.substring(0, indexOfSize)) +
                    "(\\d+)" + Pattern.quote(cpSettings.nameFormat.substring(indexOfSize + 2)) + "$";
            cpSettings.nameCompiled = Pattern.compile(cpSettings.namePattern);
        }
        if (cpSettings.alias.length() > 2) {
            throw new IllegalArgumentException("Alias '" + cpSettings.alias + "' is longer than 2 characters.");
        }
        cpSettings.colorIdx = ClaimPoints.waypointColorNames.indexOf(cpSettings.color);
        if (cpSettings.colorIdx == -1) {
            throw new IllegalArgumentException("Color '" + cpSettings.color + "' is not a valid waypoint color.");
        }
        gpSettings.firstLineCompiled = Pattern.compile(gpSettings.firstLinePattern);
        gpSettings.claimLineCompiled = Pattern.compile(gpSettings.claimLinePattern);
        gpSettings.ignoredLinesCompiled = new ArrayList<>();
        for (String str : gpSettings.ignoredLinePatterns) {
            gpSettings.ignoredLinesCompiled.add(Pattern.compile(str));
        }
        gpSettings.endingLinesCompiled = new ArrayList<>();
        for (String str : gpSettings.endingLinePatterns) {
            gpSettings.endingLinesCompiled.add(Pattern.compile(str));
        }
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
        }
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (FileReader reader = new FileReader(file.toFile())) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            ClaimPoints.LOG.error("Unable to load config.", e);
            return null;
        }
    }

    public static void save() {
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            ClaimPoints.onConfigSaved(instance);
        } catch (IOException e) {
            ClaimPoints.LOG.error("Unable to save config.", e);
        }
    }
}
