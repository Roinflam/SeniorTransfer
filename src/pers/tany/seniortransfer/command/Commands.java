package pers.tany.seniortransfer.command;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pers.tany.seniortransfer.Main;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                Main.config = IConfig.loadConfig(Main.plugin, "", "config");
                sender.sendMessage("§a重载成功");
                return true;
            }
        }
        sender.sendMessage("§a/st reload  §2重载插件");
        return true;
    }
}
