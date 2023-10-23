package pers.tany.seniortransfer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pers.tany.seniortransfer.command.Commands;
import pers.tany.seniortransfer.listenevent.Events;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;
import pers.tany.yukinoaapi.interfacepart.register.IRegister;

import java.util.ArrayList;


public class Main extends JavaPlugin {
    public static Plugin plugin = null;

    public static YamlConfiguration config;

    @Override
    public void onEnable() {
        plugin = this;

        Bukkit.getConsoleSender().sendMessage("§7[§fSeniorTransfer§7]§a已启用");

        IConfig.createResource(this, "", "config.yml", false);
        config = IConfig.loadConfig(this, "", "config");

        IRegister.registerEvents(this, new Events());
        IRegister.registerCommands(this, "SeniorTransfer", new Commands());
    }

    @Override
    public void onDisable() {
        for (String name : new ArrayList<>(Events.tpHashMap.keySet())) {
            if (Bukkit.getPlayerExact(name) != null) {
                Player player = Bukkit.getPlayerExact(name);
                Location location = Events.tpHashMap.get(name);
                Events.tpHashMap.remove(name);
                player.teleport(location);
            }
        }
        Bukkit.getConsoleSender().sendMessage("§7[§fSeniorTransfer§7]§c已卸载");
    }
}
