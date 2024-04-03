package pers.tany.seniortransfer.listenevent;


import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.seniortransfer.Main;
import pers.tany.yukinoaapi.interfacepart.location.ILocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Events implements Listener {
    public static HashMap<String, Location> tpHashMap = new HashMap<>();
    public static HashMap<String, GameMode> gameModeHashMap = new HashMap<>();
    public static ArrayList<String> allowTpArrayList = new ArrayList<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        Player player = evt.getPlayer();
        if (tpHashMap.containsKey(player.getName())) {
            player.teleport(tpHashMap.get(player.getName()));
            tpHashMap.remove(player.getName());
            player.setGameMode(gameModeHashMap.getOrDefault(player.getName(), GameMode.SURVIVAL));
            gameModeHashMap.remove(player.getName());
        }
        // 缓存的允许传送列表删除刚加入服务器的玩家
        if (Main.config.getBoolean("EnableOnlyCommand", false)) {
            allowTpArrayList.remove(player.getName());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent evt) {
        Player player = evt.getPlayer();
        if (tpHashMap.containsKey(player.getName())) {
            Location from = evt.getFrom();
            if (Main.config.getBoolean("HeadUpLimit")) {
                evt.setTo(from);
            } else {
                Location to = evt.getTo();
                if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY()) {
                    evt.setTo(from);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent evt) {
        Player player = evt.getPlayer();
        if (tpHashMap.containsKey(player.getName()) && Main.config.getBoolean("DiasbleCommand")) {
            evt.setCancelled(true);
        }
        // 使用了对应指令后加入允许传送列表
        if (Main.config.getBoolean("EnableOnlyCommand", false)) {
            for (String command : Main.config.getStringList("OnlyCommands")) {
                if (evt.getMessage().toLowerCase().contains(command.toLowerCase())) {
                    if (!allowTpArrayList.contains(player.getName())) {
                        allowTpArrayList.add(player.getName());
                    }
                    break;
                }
            }
//            if (Main.config.getStringList("OnlyCommands").contains(evt.getMessage().toLowerCase())) {
//                if (!allowTpArrayList.contains(player.getName())) {
//                    allowTpArrayList.add(player.getName());
//                }
//            }
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent evt) {
        Player player = evt.getPlayer();
        if (tpHashMap.containsKey(player.getName()) && Main.config.getBoolean("DisableChat")) {
            evt.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent evt) {
        Player player = evt.getPlayer();
        if (!player.isOp() && player.hasPermission("st.ignore")) {
            return;
        }
        if (!Main.config.getBoolean("EnableWorldAnimation") && !evt.getTo().getWorld().equals(evt.getFrom().getWorld())) {
            return;
        }
        if (Main.config.getBoolean("WhiteOrBlackList")) {
            if (Main.config.getStringList("Worlds").contains(evt.getTo().getWorld().getName())) {
                return;
            }
        } else {
            if (!Main.config.getStringList("Worlds").contains(evt.getTo().getWorld().getName())) {
                return;
            }
        }
        // 在允许传送列表内的传送才会触发动画
        if (Main.config.getBoolean("EnableOnlyCommand")) {
            if (allowTpArrayList.contains(player.getName())) {
                allowTpArrayList.remove(player.getName());
            } else {
                return;
            }
        }
        if (!tpHashMap.containsKey(player.getName()) && (evt.getCause().equals(PlayerTeleportEvent.TeleportCause.COMMAND) || evt.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN))) {
            Location from = evt.getFrom();
            GameMode gameMode = player.getGameMode();
            boolean allowFlight = player.getAllowFlight();
            boolean flying = player.isFlying();
            if (evt.getFrom().getWorld().equals(evt.getTo().getWorld())) {
                if (ILocation.getFarGrid(evt.getFrom(), evt.getTo(), false) < Main.config.getInt("GridDistance")) {
                    return;
                }
            }
            evt.getTo().getChunk().load();
            if (Main.config.getBoolean("CMIFix")) {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        player.teleport(from);

                    }
                }.runTask(Main.plugin);
            } else {
                evt.setCancelled(true);
            }
            List<Location> fromLocation = new ArrayList<>();
            List<Location> toLocation = new ArrayList<>();
            int allNumber = 0;
            for (int i = 0; i < Main.config.getInt("Number"); i++) {
                Location location = new Location(evt.getFrom().getWorld(), evt.getFrom().getX(), evt.getFrom().getWorld().getSeaLevel() + (i + 1) * Main.config.getInt("Height") + allNumber + 1, evt.getFrom().getZ(), evt.getTo().getYaw(), 90);
                if (evt.getFrom().getY() > evt.getFrom().getWorld().getSeaLevel() / 2) {
                    location = new Location(evt.getFrom().getWorld(), evt.getFrom().getX(), evt.getFrom().getY() + (i + 1) * Main.config.getInt("Height") + allNumber, evt.getFrom().getZ(), evt.getTo().getYaw(), 90);
                }
                int number = 0;
                while (evt.getFrom().getWorld().getBlockAt(location) != null && !evt.getFrom().getWorld().getBlockAt(location).getType().equals(Material.AIR)) {
                    if (number > Main.config.getInt("Height")) {
                        return;
                    }
                    location.setY(location.getY() + ++number);
                }
                allNumber += number;
                location.setY(location.getY() - 1);
                fromLocation.add(location);
            }
            for (int i = Main.config.getInt("Number"); i > 0; i--) {
                Location location = new Location(evt.getTo().getWorld(), evt.getTo().getX(), evt.getFrom().getWorld().getSeaLevel() + i * Main.config.getInt("Height") + 1, evt.getTo().getZ(), evt.getTo().getYaw(), 90);
                if (evt.getTo().getY() > evt.getTo().getWorld().getSeaLevel() / 2) {
                    location = new Location(evt.getTo().getWorld(), evt.getTo().getX(), evt.getTo().getY() + i * Main.config.getInt("Height"), evt.getTo().getZ(), evt.getTo().getYaw(), 90);
                }
                int number = 0;
                while (evt.getTo().getWorld().getBlockAt(location) != null && !evt.getTo().getWorld().getBlockAt(location).getType().equals(Material.AIR)) {
                    if (number > Main.config.getInt("Height") / 2) {
                        return;
                    }
                    location.setY(location.getY() - ++number);
                }
                location.setY(location.getY() - 1);
                toLocation.add(location);
            }
            toLocation.add(evt.getTo());
            tpHashMap.put(player.getName(), evt.getTo());

            for (int i = 0; i < fromLocation.size(); i++) {
                int finalI = i;
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        if (finalI == 0) {
                            try {
                                if (Main.config.getBoolean("DisableSpectators")) {
                                    throw new NumberFormatException();
                                }
                                player.setGameMode(GameMode.SPECTATOR);
                                new BukkitRunnable() {

                                    @Override
                                    public void run() {
                                        if (!tpHashMap.containsKey(player.getName())) {
                                            player.setGameMode(gameMode);
                                            this.cancel();
                                        } else {
                                            player.setGameMode(GameMode.SPECTATOR);
                                        }
                                    }

                                }.runTaskTimer(Main.plugin, 1, 1);
                            } catch (NoSuchFieldError exception) {
                                player.setGameMode(GameMode.SURVIVAL);
                                player.setAllowFlight(true);
                                player.setFlying(true);
                                new BukkitRunnable() {

                                    @Override
                                    public void run() {
                                        if (!tpHashMap.containsKey(player.getName())) {
                                            player.setGameMode(gameMode);
                                            player.setAllowFlight(allowFlight);
                                            player.setFlying(flying);
                                            this.cancel();
                                        } else {
                                            player.setGameMode(GameMode.SURVIVAL);
                                            player.setAllowFlight(true);
                                            player.setFlying(true);
                                        }
                                    }

                                }.runTaskTimer(Main.plugin, 1, 1);
                            }
                        }
                        player.teleport(fromLocation.get(finalI));
                    }

                }.runTaskLater(Main.plugin, (long) ((i + 1) * Main.config.getDouble("Time") * 20));
            }
            if (evt.getFrom().getWorld().equals(evt.getTo().getWorld())) {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        for (int i = 0; i < toLocation.size(); i++) {
                            int finalI = i;

                            new BukkitRunnable() {

                                @Override
                                public void run() {
                                    player.teleport(toLocation.get(finalI));
                                    if (finalI == toLocation.size() - 1) {
                                        if (player.isOnline()) {
                                            tpHashMap.remove(player.getName());
                                        } else {
                                            gameModeHashMap.put(player.getName(), gameMode);
                                        }
                                    }
                                }

                            }.runTaskLater(Main.plugin, (long) (i * Main.config.getDouble("Time") * 20));
                        }
                    }

                }.runTaskLater(Main.plugin, (long) ((fromLocation.size() + 1) * Main.config.getDouble("Time") * 30));
            } else {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        player.teleport(toLocation.get(0));
                        new BukkitRunnable() {
                            @Override
                            public void run() {

                                for (int i = 1; i < toLocation.size(); i++) {
                                    int finalI = i;

                                    new BukkitRunnable() {

                                        @Override
                                        public void run() {
                                            if (finalI == toLocation.size() - 1) {
                                                if (player.isOnline()) {
//                                                    if (Main.config.getBoolean("BackFix")) {
                                                    player.teleport(from);
//                                                    }
                                                    player.teleport(toLocation.get(finalI));
                                                    tpHashMap.remove(player.getName());
                                                } else {
                                                    gameModeHashMap.put(player.getName(), gameMode);
                                                }
                                            } else {
                                                player.teleport(toLocation.get(finalI));
                                            }
                                        }

                                    }.runTaskLater(Main.plugin, (long) (i * Main.config.getDouble("Time") * 20));
                                }
                            }

                        }.runTaskLater(Main.plugin, 20);
                    }

                }.runTaskLater(Main.plugin, (long) ((fromLocation.size() + 1) * Main.config.getDouble("Time") * 30));
            }
        }
    }

}
