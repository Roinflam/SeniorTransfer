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
    public static HashMap<String, Location> tp = new HashMap<>();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt) {
        Player player = evt.getPlayer();
        if (tp.containsKey(player.getName())) {
            player.teleport(tp.get(player.getName()));
            tp.remove(player.getName());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent evt) {
        Player player = evt.getPlayer();
        if (tp.containsKey(player.getName())) {
            evt.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent evt) {
        Player player = evt.getPlayer();
        if (tp.containsKey(player.getName())) {
            evt.setCancelled(true);
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent evt) {
        Player player = evt.getPlayer();
        if (tp.containsKey(player.getName())) {
            evt.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent evt) {
        Player player = evt.getPlayer();
        if (!Main.config.getBoolean("EnableWorldAnimation") && !evt.getTo().getWorld().equals(evt.getFrom().getWorld())) {
            return;
        }
        if (Main.config.getStringList("DisableWorlds").contains(evt.getTo().getWorld().getName())) {
            return;
        }
        evt.getTo().getChunk().load();
        if (!tp.containsKey(player.getName()) && (evt.getCause().equals(PlayerTeleportEvent.TeleportCause.COMMAND) || evt.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN))) {
            if (evt.getFrom().getWorld().equals(evt.getTo().getWorld())) {
                if (ILocation.getFarGrid(evt.getFrom(), evt.getTo(), false) < Main.config.getInt("GridDistance")) {
                    return;
                }
            }
            evt.setCancelled(true);
            new BukkitRunnable() {

                @Override
                public void run() {
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
                    tp.put(player.getName(), evt.getTo());

                    for (int i = 0; i < fromLocation.size(); i++) {
                        int finalI = i;
                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                if (finalI == 0) {
                                    try {
                                        GameMode gameMode = player.getGameMode();
                                        player.setGameMode(GameMode.SPECTATOR);
                                        new BukkitRunnable() {

                                            @Override
                                            public void run() {
                                                if (!tp.containsKey(player.getName())) {
                                                    player.setGameMode(gameMode);
                                                    this.cancel();
                                                } else {
                                                    player.setGameMode(GameMode.SPECTATOR);
                                                }
                                            }

                                        }.runTaskTimer(Main.plugin, 1, 1);
                                    } catch (Exception ignored) {

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
                                                tp.remove(player.getName());
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
                                                    player.teleport(toLocation.get(finalI));
                                                    if (finalI == toLocation.size() - 1) {
                                                        tp.remove(player.getName());
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

            }.runTask(Main.plugin);
        }
    }

}
