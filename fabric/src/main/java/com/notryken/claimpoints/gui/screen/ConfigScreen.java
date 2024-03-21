package com.notryken.claimpoints.gui.screen;

import com.notryken.claimpoints.ClaimPoints;
import com.notryken.claimpoints.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen {
    public static Screen getConfigScreen(Config config, Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("ClaimPoints Settings"))
                .setSavingRunnable(() -> {
                    try {
                        ClaimPoints.config().verifyConfig();
                        ClaimPoints.config().writeToFile();
                    }
                    catch (IllegalArgumentException e) {
                        ClaimPoints.LOG.warn("Invalid config.", e);
                        ClaimPoints.LOG.info("Using default configuration.");
                        ClaimPoints.restoreDefaultConfig();
                    }
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory claimPoints = builder.getOrCreateCategory(Component.literal("ClaimPoints"));

        claimPoints.addEntry(eb.startStrField(Component.literal(
                "ClaimPoint Name Format (must contain %d for claim size)"),
                        config.cpSettings.nameFormat)
                .setDefaultValue(Config.DEFAULT_CLAIMPOINT_FORMAT)
                .setSaveConsumer(var -> config.cpSettings.nameFormat = var)
                .build());

        claimPoints.addEntry(eb.startStrField(Component.literal("ClaimPoint Alias"), config.cpSettings.alias)
                .setDefaultValue(Config.DEFAULT_CLAIMPOINT_ALIAS)
                .setSaveConsumer(var -> config.cpSettings.alias = var.length() <= 2 ? var : var.substring(0, 2))
                .build());

        claimPoints.addEntry(eb.startStringDropdownMenu(Component.literal("ClaimPoint Color Name"), config.cpSettings.color)
                .setDefaultValue(Config.DEFAULT_CLAIMPOINT_COLOR)
                .setSelections(ClaimPoints.waypointColorNames)
                .setSaveConsumer(var -> config.cpSettings.color = var)
                .build());


        ConfigCategory griefPrevention = builder.getOrCreateCategory(Component.literal("GriefPrevention"));

        griefPrevention.addEntry(eb.startStrField(Component.literal("First Line Pattern"), config.gpSettings.firstLinePattern)
                .setDefaultValue(Config.DEFAULT_FIRST_LINE_PATTERN)
                .setSaveConsumer(var -> config.gpSettings.firstLinePattern = var)
                .build());

        griefPrevention.addEntry(eb.startStrField(Component.literal("Claim Line Pattern"), config.gpSettings.claimLinePattern)
                .setDefaultValue(Config.DEFAULT_CLAIM_LINE_PATTERN)
                .setSaveConsumer(var -> config.gpSettings.claimLinePattern = var)
                .build());

        griefPrevention.addEntry(eb.startStrList(Component.literal("Ignored Line Patterns"), config.gpSettings.ignoredLinePatterns)
                .setDefaultValue(Config.DEFAULT_IGNORED_LINE_PATTERNS)
                .setSaveConsumer(var -> config.gpSettings.ignoredLinePatterns = var)
                .build());

        griefPrevention.addEntry(eb.startStrList(Component.literal("Ending Line Patterns"), config.gpSettings.endingLinePatterns)
                .setDefaultValue(Config.DEFAULT_ENDING_LINE_PATTERNS)
                .setSaveConsumer(var -> config.gpSettings.endingLinePatterns = var)
                .build());

        return builder.build();
    }
}
