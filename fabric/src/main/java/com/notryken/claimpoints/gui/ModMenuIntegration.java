package com.notryken.claimpoints.gui;

import com.notryken.claimpoints.ClaimPoints;
import com.notryken.claimpoints.gui.screen.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return new qmConfigScreenFactory();
    }

    private static class qmConfigScreenFactory implements ConfigScreenFactory<Screen> {
        public Screen create(Screen screen) {
            return ConfigScreen.getConfigScreen(ClaimPoints.config(), screen);
        }
    }
}