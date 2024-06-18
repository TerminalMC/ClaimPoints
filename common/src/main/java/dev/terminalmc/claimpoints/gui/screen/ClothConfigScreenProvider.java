package dev.terminalmc.claimpoints.gui.screen;

import dev.terminalmc.claimpoints.ClaimPoints;
import dev.terminalmc.claimpoints.config.Config;
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
                        Config.get().verify();
                        Config.save();
                    } catch (IllegalArgumentException e) {
                        ClaimPoints.LOG.warn("Invalid config.", e);
                        ClaimPoints.LOG.info("Reverting to default configuration.");
                        Config.resetAndSave();
                    }
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory cp = builder.getOrCreateCategory(Component.literal("ClaimPoints"));

        cp.addEntry(eb.startStrField(Component.literal(
                                "ClaimPoint Name Format (must contain %d for claim size)"),
                        config.cpSettings.nameFormat)
                .setDefaultValue(Config.ClaimPointSettings.defaultNameFormat)
                .setSaveConsumer(var -> config.cpSettings.nameFormat = var)
                .build());

        cp.addEntry(eb.startStrField(Component.literal("ClaimPoint Alias"), config.cpSettings.alias)
                .setDefaultValue(Config.ClaimPointSettings.defaultAlias)
                .setSaveConsumer(var -> config.cpSettings.alias = var.length() <= 2 ? var : var.substring(0, 2))
                .build());

        cp.addEntry(eb.startDropdownMenu(Component.literal("ClaimPoint Color"),
                        config.cpSettings.color, String::valueOf)
                .setSuggestionMode(false)
                .setSelections(ClaimPoints.waypointColorNames)
                .setDefaultValue(Config.ClaimPointSettings.defaultColor)
                .setSaveConsumer(var -> config.cpSettings.color = var)
                .build());


        ConfigCategory gp = builder.getOrCreateCategory(Component.literal("GriefPrevention"));

        gp.addEntry(eb.startStrField(Component.literal("First Line Pattern"), config.gpSettings.firstLinePattern)
                .setDefaultValue(Config.GriefPreventionSettings.defaultFirstLinePattern)
                .setSaveConsumer(var -> config.gpSettings.firstLinePattern = var)
                .build());

        gp.addEntry(eb.startStrField(Component.literal("Claim Line Pattern"), config.gpSettings.claimLinePattern)
                .setDefaultValue(Config.GriefPreventionSettings.defaultClaimLinePattern)
                .setSaveConsumer(var -> config.gpSettings.claimLinePattern = var)
                .build());

        gp.addEntry(eb.startStrList(Component.literal("Ignored Line Patterns"), config.gpSettings.ignoredLinePatterns)
                .setDefaultValue(Config.GriefPreventionSettings.defaultIgnoredLinePatterns)
                .setSaveConsumer(var -> config.gpSettings.ignoredLinePatterns = var)
                .build());

        gp.addEntry(eb.startStrList(Component.literal("Ending Line Patterns"), config.gpSettings.endingLinePatterns)
                .setDefaultValue(Config.GriefPreventionSettings.defaultEndingLinePatterns)
                .setSaveConsumer(var -> config.gpSettings.endingLinePatterns = var)
                .build());

        return builder.build();
    }
}
