package com.notryken.claimpoints.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.notryken.claimpoints.ClaimPoints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Includes derivative work of code used by
 * <a href="https://github.com/CaffeineMC/sodium-fabric/">Sodium</a>
 */
public class Config {
    private static final String DEFAULT_FILE_NAME = "claimpoints.json";
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();

    public static final String DEFAULT_FIRST_LINE_PATTERN = "^-?\\d+ blocks from play \\+ -?\\d+ bonus = -?\\d+ total.$";
    public static final String DEFAULT_CLAIM_LINE_PATTERN = "^(.+): x(-?\\d+), z(-?\\d+) \\(-?(\\d+) blocks\\)$";
    public static final List<String> DEFAULT_IGNORED_LINE_PATTERNS = List.of(
            "^Claims:$"
    );
    public static final List<String> DEFAULT_ENDING_LINE_PATTERNS = List.of(
            "^ = -?\\d* blocks left to spend$"
    );

    private static Path configPath;

    public final TextSettings text = new TextSettings();

    public static class TextSettings {
        public String firstLinePattern = DEFAULT_FIRST_LINE_PATTERN;
        public transient Pattern firstLineCompiled;
        public String claimLinePattern = DEFAULT_CLAIM_LINE_PATTERN;
        public transient Pattern claimLineCompiled;
        public List<String> ignoredLinePatterns = new ArrayList<>(DEFAULT_IGNORED_LINE_PATTERNS);
        public transient List<Pattern> ignoredLinesCompiled;
        public List<String> endingLinePatterns = new ArrayList<>(DEFAULT_ENDING_LINE_PATTERNS);
        public transient List<Pattern> endingLinesCompiled;
    }

    public void createPatterns() {
        this.text.firstLineCompiled = Pattern.compile(text.firstLinePattern);
        this.text.claimLineCompiled = Pattern.compile(text.claimLinePattern);
        this.text.ignoredLinesCompiled = new ArrayList<>();
        for (String str : this.text.ignoredLinePatterns) {
            this.text.ignoredLinesCompiled.add(Pattern.compile(str));
        }
        this.text.endingLinesCompiled = new ArrayList<>();
        for (String str : this.text.endingLinePatterns) {
            this.text.endingLinesCompiled.add(Pattern.compile(str));
        }
    }

    public static @NotNull Config load() {
        Config config = load(DEFAULT_FILE_NAME);

        if (config == null) {
            ClaimPoints.LOG.info("Using default configuration.");
            config = new Config();
            config.createPatterns();
        }
        else {
            try {
                config.createPatterns();
            }
            catch (PatternSyntaxException e) {
                ClaimPoints.LOG.warn("Invalid regex in config.", e);
                ClaimPoints.LOG.info("Using default configuration.");
                config = new Config();
                config.createPatterns();
            }
        }

        config.writeToFile();

        return config;
    }

    public static @Nullable Config load(String name) {
        configPath = Path.of("config").resolve(name);
        Config config = null;

        if (Files.exists(configPath)) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                config = GSON.fromJson(reader, Config.class);
            } catch (Exception e) {
                ClaimPoints.LOG.error("Unable to load config from file '{}'.", configPath, e);
            }
        } else {
            ClaimPoints.LOG.warn("Unable to locate config file '{}'.", name);
        }
        return config;
    }

    public void writeToFile() {
        Path dir = configPath.getParent();

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            else if (!Files.isDirectory(dir)) {
                throw new IOException("Not a directory: " + dir);
            }

            // Use a temporary location next to the config's final destination
            Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");

            // Write the file to the temporary location
            Files.writeString(tempPath, GSON.toJson(this));

            // Atomically replace the old config file (if it exists) with the temporary file
            Files.move(tempPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to update config file.", e);
        }
    }
}
