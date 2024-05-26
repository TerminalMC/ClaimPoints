package com.notryken.claimpoints.gui.screen;

import com.notryken.claimpoints.ClaimPoints;
import com.notryken.claimpoints.config.Config;
import me.shedaniel.clothconfig2.api.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfigScreenProvider {
    /**
     * Builds and returns a Cloth Config options screen.
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the Cloth Config API mod is not
     * available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config config = Config.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("ClaimPoints Settings"))
                .setSavingRunnable(() -> {
                    try {
                        Config.get().verifyConfig();
                        Config.save();
                    } catch (IllegalArgumentException e) {
                        ClaimPoints.LOG.warn("Invalid config.", e);
                        ClaimPoints.LOG.info("Reverting to default configuration.");
                        Config.resetAndSave();
                    }
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory claimPoints = builder.getOrCreateCategory(Component.literal("ClaimPoints"));

        claimPoints.addEntry(eb.startStrField(Component.literal(
                                "ClaimPoint Name Format (must contain %d for claim size)"),
                        config.cpSettings.nameFormat)
                .setDefaultValue(Config.ClaimPointSettings.defaultNameFormat)
                .setSaveConsumer(var -> config.cpSettings.nameFormat = var)
                .build());

        claimPoints.addEntry(eb.startStrField(Component.literal("ClaimPoint Alias"), config.cpSettings.alias)
                .setDefaultValue(Config.ClaimPointSettings.defaultAlias)
                .setSaveConsumer(var -> config.cpSettings.alias = var.length() <= 2 ? var : var.substring(0, 2))
                .build());

        claimPoints.addEntry(eb.startStringDropdownMenu(Component.literal("ClaimPoint Color"), config.cpSettings.color)
                .setDefaultValue(Config.ClaimPointSettings.defaultColor)
                .setSelections(ClaimPoints.waypointColorNames)
                .setSaveConsumer(var -> config.cpSettings.color = var)
                .build());


        ConfigCategory griefPrevention = builder.getOrCreateCategory(Component.literal("GriefPrevention"));

        griefPrevention.addEntry(eb.startStrField(Component.literal("First Line Pattern"), config.gpSettings.firstLinePattern)
                .setDefaultValue(Config.GriefPreventionSettings.defaultFirstLinePattern)
                .setSaveConsumer(var -> config.gpSettings.firstLinePattern = var)
                .build());

        griefPrevention.addEntry(eb.startStrField(Component.literal("Claim Line Pattern"), config.gpSettings.claimLinePattern)
                .setDefaultValue(Config.GriefPreventionSettings.defaultClaimLinePattern)
                .setSaveConsumer(var -> config.gpSettings.claimLinePattern = var)
                .build());

        griefPrevention.addEntry(eb.startStrList(Component.literal("Ignored Line Patterns"), config.gpSettings.ignoredLinePatterns)
                .setDefaultValue(Config.GriefPreventionSettings.defaultIgnoredLinePatterns)
                .setSaveConsumer(var -> config.gpSettings.ignoredLinePatterns = var)
                .build());

        griefPrevention.addEntry(eb.startStrList(Component.literal("Ending Line Patterns"), config.gpSettings.endingLinePatterns)
                .setDefaultValue(Config.GriefPreventionSettings.defaultEndingLinePatterns)
                .setSaveConsumer(var -> config.gpSettings.endingLinePatterns = var)
                .build());

        return builder.build();
    }
}
