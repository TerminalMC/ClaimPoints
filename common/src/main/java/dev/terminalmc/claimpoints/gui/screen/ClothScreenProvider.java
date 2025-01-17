/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.claimpoints.gui.screen;

import dev.terminalmc.claimpoints.ClaimPoints;
import dev.terminalmc.claimpoints.config.Config;
import me.shedaniel.clothconfig2.api.*;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

import static dev.terminalmc.claimpoints.util.Localization.localized;

public class ClothScreenProvider {
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
                .setTitle(localized("name"))
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

        cp.addEntry(eb.startStrField(localized("option", "nameFormat"), config.cpSettings.nameFormat)
                .setDefaultValue(Config.ClaimPointSettings.nameFormatDefault)
                .setErrorSupplier(val -> {
                    if (val.contains("%d")) return Optional.empty();
                    else return Optional.of(localized("option", "nameFormat.error"));
                })
                .setSaveConsumer(var -> config.cpSettings.nameFormat = var)
                .build());

        cp.addEntry(eb.startStrField(localized("option", "alias"), config.cpSettings.alias)
                .setDefaultValue(Config.ClaimPointSettings.aliasDefault)
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
                .setDefaultValue(Config.ClaimPointSettings.colorDefault)
                .setSaveConsumer(var -> config.cpSettings.color = var)
                .build());


        ConfigCategory gp = builder.getOrCreateCategory(localized(
                "option", "griefprevention"));

        gp.addEntry(eb.startStrField(localized("option", "command"),
                        config.gpSettings.claimListCommand)
                .setDefaultValue(Config.GriefPreventionSettings.claimListCommandDefault)
                .setSaveConsumer(var -> config.gpSettings.claimListCommand = var)
                .build());

        gp.addEntry(eb.startStrField(localized("option", "firstPattern"),
                        config.gpSettings.firstLinePattern)
                .setDefaultValue(Config.GriefPreventionSettings.firstLinePatternDefault)
                .setSaveConsumer(var -> config.gpSettings.firstLinePattern = var)
                .build());

        gp.addEntry(eb.startStrField(localized("option", "claimPattern"),
                        config.gpSettings.claimLinePattern)
                .setDefaultValue(Config.GriefPreventionSettings.claimLinePatternDefault)
                .setSaveConsumer(var -> config.gpSettings.claimLinePattern = var)
                .build());

        gp.addEntry(eb.startStrList(localized("option", "ignoredPattern"),
                        config.gpSettings.ignoredLinePatterns)
                .setDefaultValue(Config.GriefPreventionSettings.ignoredLinePatternsDefault)
                .setSaveConsumer(var -> config.gpSettings.ignoredLinePatterns = var)
                .build());

        gp.addEntry(eb.startStrList(localized("option", "endingPattern"),
                        config.gpSettings.endingLinePatterns)
                .setDefaultValue(Config.GriefPreventionSettings.endingLinePatternsDefault)
                .setSaveConsumer(var -> config.gpSettings.endingLinePatterns = var)
                .build());

        ConfigCategory ac = builder.getOrCreateCategory(localized(
                "option", "autocommands"));

        ac.addEntry(eb.startIntSlider(localized("option", "autocommands.commandDelay"), 
                        config.acSettings.commandDelay, 1, 60)
                .setTooltip(localized("option", "autocommands.commandDelay.tooltip"))
                .setDefaultValue(Config.AutoCommandSettings.commandDelayDefault)
                .setSaveConsumer(val -> config.acSettings.commandDelay = val)
                .setTextGetter(val -> localized("option", "autocommands.commandDelay.value", val))
                .build());

        ac.addEntry(eb.startStrList(localized("option", "autocommands.servers"),
                        config.acSettings.servers)
                .setTooltip(localized("option", "autocommands.servers.tooltip"))
                .setExpanded(true)
                .setDefaultValue(Config.AutoCommandSettings.serversDefault)
                .setSaveConsumer(var -> config.acSettings.servers = var)
                .build());

        ac.addEntry(eb.startStrList(localized("option", "autocommands.commands"),
                        config.acSettings.commands)
                .setTooltip(localized("option", "autocommands.commands.tooltip"))
                .setExpanded(true)
                .setDefaultValue(Config.AutoCommandSettings.commandsDefault)
                .setSaveConsumer(var -> config.acSettings.commands = var)
                .build());

        return builder.build();
    }
}
