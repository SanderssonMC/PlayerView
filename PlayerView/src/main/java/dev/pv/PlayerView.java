package dev.pv;

import dev.pv.command.PvCommand;
import dev.pv.config.PvConfig;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * PlayerView - a client-side BedWars stats viewer for Hypixel.
 *
 * Usage in game:
 *   /pv <player>          open the full stats GUI
 *   /pv <player> -c       print the compact chat card
 *   /pv key <hypixelKey>  save your Hypixel API key
 *   /pv sukie <sukieKey>  save your sukie.net API key
 */
@Mod(modid = PlayerView.MODID, name = PlayerView.NAME, version = PlayerView.VERSION, clientSideOnly = true)
public class PlayerView {

    public static final String MODID = "playerview";
    public static final String NAME = "PlayerView";
    public static final String VERSION = "1.0.0";

    public static PvConfig config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Config lives in <gamedir>/config/playerview.cfg
        config = new PvConfig(event.getModConfigurationDirectory());
        config.load();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new PvCommand());
    }
}
