package com.notryken.claimpoints.gui.screen;

import com.notryken.claimpoints.ClaimPoints;
import com.notryken.claimpoints.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.regex.PatternSyntaxException;

public class ConfigScreen {
    public static Screen getConfigScreen(Config config, Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("ClaimPoints Settings"))
                .setSavingRunnable(() -> {
                    try {
                        ClaimPoints.config().createPatterns();
                        ClaimPoints.config().writeToFile();
                    }
                    catch (PatternSyntaxException e) {
                        ClaimPoints.LOG.warn("Invalid regex in config.", e);
                        ClaimPoints.LOG.info("Using default configuration.");
                        ClaimPoints.restoreDefaultConfig();
                    }
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory claimPoints = builder.getOrCreateCategory(Component.literal("ClaimPoints"));

        claimPoints.addEntry(eb.startStrField(Component.literal("ClaimPoint Name Format"), config.gpSettings.firstLinePattern)
                .setDefaultValue(Config.DEFAULT_CLAIMPOINT_FORMAT).setSaveConsumer(var -> config.cpSettings.nameFormat = var).build());

        claimPoints.addEntry(eb.startStrField(Component.literal("ClaimPoint Name Pattern"), config.gpSettings.firstLinePattern)
                .setDefaultValue(Config.DEFAULT_CLAIMPOINT_PATTERN).setSaveConsumer(var -> config.cpSettings.namePattern = var).build());


        ConfigCategory griefPrevention = builder.getOrCreateCategory(Component.literal("GriefPrevention"));

        griefPrevention.addEntry(eb.startStrField(Component.literal("First Line Pattern"), config.gpSettings.firstLinePattern)
                .setDefaultValue(Config.DEFAULT_FIRST_LINE_PATTERN).setSaveConsumer(var -> config.gpSettings.firstLinePattern = var).build());

        griefPrevention.addEntry(eb.startStrField(Component.literal("Claim Line Pattern"), config.gpSettings.claimLinePattern)
                .setDefaultValue(Config.DEFAULT_CLAIM_LINE_PATTERN).setSaveConsumer(var -> config.gpSettings.claimLinePattern = var).build());

        griefPrevention.addEntry(eb.startStrList(Component.literal("Ignored Line Patterns"), config.gpSettings.ignoredLinePatterns)
                .setDefaultValue(Config.DEFAULT_IGNORED_LINE_PATTERNS).setSaveConsumer(var -> config.gpSettings.ignoredLinePatterns = var).build());

        griefPrevention.addEntry(eb.startStrList(Component.literal("Ending Line Patterns"), config.gpSettings.endingLinePatterns)
                .setDefaultValue(Config.DEFAULT_ENDING_LINE_PATTERNS).setSaveConsumer(var -> config.gpSettings.endingLinePatterns = var).build());

        return builder.build();
    }
}
