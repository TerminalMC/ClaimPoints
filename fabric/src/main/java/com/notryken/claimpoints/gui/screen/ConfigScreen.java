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

        ConfigCategory text = builder.getOrCreateCategory(Component.literal("Text"));

        text.addEntry(eb.startStrField(Component.literal("First Line Pattern"), config.text.firstLinePattern)
                .setDefaultValue(Config.DEFAULT_FIRST_LINE_PATTERN).setSaveConsumer(var -> config.text.firstLinePattern = var).build());

        text.addEntry(eb.startStrField(Component.literal("Claim Line Pattern"), config.text.claimLinePattern)
                .setDefaultValue(Config.DEFAULT_CLAIM_LINE_PATTERN).setSaveConsumer(var -> config.text.claimLinePattern = var).build());

        text.addEntry(eb.startStrList(Component.literal("Ignored Line Patterns"), config.text.ignoredLinePatterns)
                .setDefaultValue(Config.DEFAULT_IGNORED_LINE_PATTERNS).setSaveConsumer(var -> config.text.ignoredLinePatterns = var).build());

        text.addEntry(eb.startStrList(Component.literal("Ending Line Patterns"), config.text.endingLinePatterns)
                .setDefaultValue(Config.DEFAULT_ENDING_LINE_PATTERNS).setSaveConsumer(var -> config.text.endingLinePatterns = var).build());

        return builder.build();
    }
}
