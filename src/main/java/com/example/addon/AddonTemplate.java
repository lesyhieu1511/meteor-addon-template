package com.example.addon;

import com.example.addon.commands.CommandExample;
import com.example.addon.hud.HudExample;
import com.example.addon.modules.AimAssist;
import com.example.addon.modules.AutoDoubleHand;
import com.example.addon.modules.AutoInvTotem;
import com.example.addon.modules.GlazedFreecam;
import com.example.addon.modules.HoverTotem;
import com.example.addon.modules.ModuleExample;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Glazed Port");
    public static final HudGroup HUD_GROUP = new HudGroup("Glazed Port");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Glazed 26.2 port");
        Modules.get().add(new GlazedFreecam());
        Modules.get().add(new AutoDoubleHand());
        Modules.get().add(new AutoInvTotem());
        Modules.get().add(new HoverTotem());
        Modules.get().add(new AimAssist());
        Modules.get().add(new ModuleExample());
        Commands.add(new CommandExample());
        Hud.get().register(HudExample.INFO);
    }

    @Override public void onRegisterCategories() { Modules.registerCategory(CATEGORY); }
    @Override public String getPackage() { return "com.example.addon"; }
    @Override public GithubRepo getRepo() { return new GithubRepo("lesyhieu1511", "meteor-addon-template"); }
}
