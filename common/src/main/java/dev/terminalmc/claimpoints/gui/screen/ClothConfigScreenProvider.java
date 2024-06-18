package dev.terminalmc.claimpoints.gui.screen;

import dev.terminalmc.claimpoints.ClaimPoints;
import dev.terminalmc.claimpoints.config.Config;
import me.shedaniel.clothconfig2.api.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

import static dev.terminalmc.claimpoints.util.Localization.localized;

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

        ConfigCategory cp = builder.getOrCreateCategory(localized(
                "option", "waypoints"));

        cp.addEntry(eb.startStrField(localized("option", "name_format"), config.cpSettings.nameFormat)
                .setDefaultValue(Config.ClaimPointSettings.defaultNameFormat)
                .setErrorSupplier(val -> {
                    if (val.contains("%d")) return Optional.empty();
                    else return Optional.of(localized("option", "name_format.error"));
                })
                .setSaveConsumer(var -> config.cpSettings.nameFormat = var)
                .build());

        cp.addEntry(eb.startStrField(localized("option", "alias"), config.cpSettings.alias)
                .setDefaultValue(Config.ClaimPointSettings.defaultAlias)
                .setErrorSupplier(val -> {
                    if (!val.isEmpty() && val.length() < 3) return Optional.empty();
                    else return Optional.of(localized("option", "alias.error"));
                })
                .setSaveConsumer(var -> config.cpSettings.alias = var)
                .build());

        cp.addEntry(eb.startDropdownMenu(localized("option", "color"),
                        config.cpSettings.color, String::valueOf)
                .setSuggestionMode(false)
                .setSelections(ClaimPoints.waypointColorNames)
                .setDefaultValue(Config.ClaimPointSettings.defaultColor)
                .setSaveConsumer(var -> config.cpSettings.color = var)
                .build());


        ConfigCategory gp = builder.getOrCreateCategory(localized(
                "option", "griefprevention"));

        gp.addEntry(eb.startStrField(localized("option", "command"),
                        config.gpSettings.claimListCommand)
                .setDefaultValue(Config.GriefPreventionSettings.defaultClaimListCommand)
                .setSaveConsumer(var -> config.gpSettings.claimListCommand = var)
                .build());

        gp.addEntry(eb.startStrField(localized("option", "first_pattern"),
                        config.gpSettings.firstLinePattern)
                .setDefaultValue(Config.GriefPreventionSettings.defaultFirstLinePattern)
                .setSaveConsumer(var -> config.gpSettings.firstLinePattern = var)
                .build());

        gp.addEntry(eb.startStrField(localized("option", "claim_pattern"),
                        config.gpSettings.claimLinePattern)
                .setDefaultValue(Config.GriefPreventionSettings.defaultClaimLinePattern)
                .setSaveConsumer(var -> config.gpSettings.claimLinePattern = var)
                .build());

        gp.addEntry(eb.startStrList(localized("option", "ignored_pattern"),
                        config.gpSettings.ignoredLinePatterns)
                .setDefaultValue(Config.GriefPreventionSettings.defaultIgnoredLinePatterns)
                .setSaveConsumer(var -> config.gpSettings.ignoredLinePatterns = var)
                .build());

        gp.addEntry(eb.startStrList(localized("option", "ending_pattern"),
                        config.gpSettings.endingLinePatterns)
                .setDefaultValue(Config.GriefPreventionSettings.defaultEndingLinePatterns)
                .setSaveConsumer(var -> config.gpSettings.endingLinePatterns = var)
                .build());

        return builder.build();
    }
}
