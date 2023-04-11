package me.KP56.FakePlayers;

import de.jeff_media.updatechecker.UpdateChecker;
import me.KP56.FakePlayers.commands.FakePlayers;
import me.KP56.FakePlayers.listeners.EventListener;
import me.KP56.FakePlayers.multiversion.Version;
import me.KP56.FakePlayers.socket.FakePlayersSocket;
import me.KP56.FakePlayers.TabComplete.FakePlayersTabComplete;
import me.KP56.FakePlayers.Utils.Color;
import me.KP56.FakePlayers.bstats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class FakePlugin extends JavaPlugin {

    private static final int SPIGOT_RESOURCE_ID = 91163;
    private static FakePlugin plugin;
    public FileConfiguration config;
    private Metrics metrics;

    private boolean usesCraftBukkit = false;
    private boolean usesPaper = false;
    private boolean updatedPaper = false;
    private boolean usesProtocolLib = false;
    private boolean usesFastLogin = false;
    private boolean usesAuthMe = false;
    private boolean usesHamsterAPI = false;
    private boolean usesNexEngine = false;
    private boolean usesCustomDisplay = false;

    private BukkitRunnable ticker;

    private Version version = Version.valueOf(Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]);

    public static FakePlugin getPlugin() {
        return plugin;
    }

    public static String getConfigMessage(FileConfiguration config, String path, String[] args) {
        String text = config.getString(path);

        boolean open = false;
        StringBuilder chars = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '%') {
                if (open) {
                    final char[] CHARACTERS = chars.toString().toCharArray();
                    if (CHARACTERS[0] == 'a' && CHARACTERS[1] == 'r' && CHARACTERS[2] == 'g') {
                        final int ARG = Integer.parseInt(String.valueOf(CHARACTERS[3]));

                        text = text.replace(chars.toString(), args[ARG]);

                        chars = new StringBuilder();
                    }
                    open = false;
                } else {
                    open = true;
                }
                continue;
            }

            if (open) {
                chars.append(c);
            }
        }

        return Color.format(config.getString("prefix") + " " + text.replace("%", ""));
    }

    public boolean usesPaper() {
        return usesPaper;
    }

    public boolean isPaperUpdated() {
        return updatedPaper;
    }

    public Version getVersion() {
        return version;
    }

    private void checkForClasses() {
        try {
            usesPaper = Class.forName("com.destroystokyo.paper.VersionHistoryManager$VersionData") != null;

            if (usesPaper) {
                Bukkit.getLogger().info("Paper detected.");
            }
        } catch (ClassNotFoundException ignored) {

        }

        try {
            updatedPaper = Class.forName("net.kyori.adventure.text.ComponentLike") != null;
        } catch (ClassNotFoundException ignored) {

        }

        try {
            usesCraftBukkit = Class.forName("org.spigotmc.SpigotConfig") == null;
        } catch (ClassNotFoundException ignored) {
            usesCraftBukkit = true;
        }

        try {
            this.usesProtocolLib = Class.forName("com.comphenix.protocol.ProtocolLib") != null;
        } catch (ClassNotFoundException ignored) {

        }

        try {
            this.usesFastLogin = Class.forName("com.github.games647.fastlogin.bukkit.FastLoginBukkit") != null;
        } catch (ClassNotFoundException ignored) {

        }

        try {
            this.usesHamsterAPI = Class.forName("dev._2lstudios.hamsterapi.HamsterAPI") != null;
        } catch (ClassNotFoundException ignored) {

        }

        try {
            this.usesAuthMe = Class.forName("fr.xephi.authme.AuthMe") != null;
        } catch (ClassNotFoundException ignored) {

        }

        try {
            this.usesNexEngine = Class.forName("su.nexmedia.engine.NexPlugin") != null;
        } catch (ClassNotFoundException ignored) {

        }

        try {
            this.usesCustomDisplay = Class.forName("com.daxton.customdisplay.CustomDisplay") != null;
        } catch (ClassNotFoundException ignored) {

        }
    }

    @Override
    public void onEnable() {

        File macrosFolder = new File("plugins/FakePlayers/macros");
        if (!macrosFolder.exists()) {
            macrosFolder.mkdir();
        }

        File cacheFolder = new File("plugins/FakePlayers/cache");
        if (!cacheFolder.exists()) {
            cacheFolder.mkdir();
        }

        Bukkit.getLogger().info("Detected version: " + version.name());
        if (version == null) {
            Bukkit.getLogger().warning("This server version is not supported by Fake Players!");
        }



        getCommand("fakeplayers").setExecutor(new FakePlayers());
        getCommand("fakeplayers").setTabCompleter(new FakePlayersTabComplete());

        getServer().getPluginManager().registerEvents(new EventListener(), this);

        plugin = this;

        checkForClasses();

        load();

        if (config.getBoolean("update-notifications") && !usesCraftBukkit) {
            UpdateChecker.init(this, SPIGOT_RESOURCE_ID)
                    .setDownloadLink(SPIGOT_RESOURCE_ID)
                    .setNotifyByPermissionOnJoin("fakeplayers.notify")
                    .setNotifyOpsOnJoin(true)
                    .checkEveryXHours(6)
                    .checkNow();
        }

        if (config.getBoolean("bstats")) {
            metrics = new Metrics(this, 11025);
        }
        if (config.getBoolean("bungeecord.enabled")) {
            System.out.println("Starting socket...");
            FakePlayersSocket.fakePlayersSocket.start(config.getString("bungeecord.ip"), config.getInt("bungeecord.fakeplayers-port"));
        }
    }

    public void load() {
        this.saveDefaultConfig();
        config = this.getConfig();
        validateConfig();

        if (!config.getBoolean("bungeecord.enabled")) {
            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                File cache = new File(getDataFolder(), "cache/cache.fpcache");
                if (cache.exists()) {
                    try(BufferedReader reader = new BufferedReader(new FileReader(new File(getDataFolder(), "cache/cache.fpcache")));){
                        String line = reader.readLine();
                        while (line != null) {
                            String[] split = line.split(";");
                            if(split.length < 2) {
                                FakePlayer.create(line);
                            }
                            else {
                                FakePlayer.summon(UUID.fromString(split[0]), split[1]);
                            }

                            line = reader.readLine();
                        }
                    } catch (IOException e) {
                        Bukkit.getLogger().warning("Failed to read from cache. Fake players from last server instance won't rejoin.");
                    }

                    cache.delete();
                }
            });
        }

        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                Collection<FakePlayer> players = new HashSet<>(FakePlayer.getFakePlayers().values());
                for(FakePlayer player : players) {
                    player.getEntityPlayer().playerTick();
                }
            }
        };
        ticker.runTaskTimer(this, 100L, 1L);
    }

    public void loadPlayers() {

    }

    public boolean usesCraftBukkit() {
        return usesCraftBukkit;
    }

    @Override
    public void onDisable() {
        unload();
        metrics.shutdown();
    }

    public void reload() {
        ticker.cancel();
        unload();
        reloadConfig();
        load();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::loadPlayers);
    }

    public void unload() {
        Map<UUID, FakePlayer> copyList = new HashMap<>(FakePlayer.getFakePlayers());
        //plugins/FakePlayers/cache/cache$1.fpcache
        try(BufferedWriter myWriter = new BufferedWriter(new FileWriter(new File(getDataFolder(), "cache/cache.fpcache")))){
            for (FakePlayer player : copyList.values()) {
                myWriter.write(player.getUUID().toString() + ";" + player.getName() + "\n");
                player.removePlayer();
            }
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to cache fake players who are currently online. They will not rejoin your server.", e);
        }
        Bukkit.getScheduler().cancelTasks(this);
    }

    private void validateConfig() {
        InputStream is = getResource("config.yml");

        if(is == null) {
            getLogger().warning("Could not validate config.yml!");
            return;
        }
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(is));

        Set<String> pluginKeys = configuration.getKeys(true);
        Set<String> configKeys = config.getKeys(true);

        for (String s : pluginKeys) {
            if (!configKeys.contains(s)) {
                System.out.println("You are using an invalid version of Fake Players config. Creating a new one...");
                new File(getDataFolder(), "config.yml").delete();
                this.saveDefaultConfig();

                return;
            }
        }
    }

    public boolean usesProtocolLib() {
        return usesProtocolLib;
    }

    public boolean usesFastLogin() {
        return usesFastLogin;
    }

    public boolean usesAuthMe() {
        return usesAuthMe;
    }

    public boolean usesHamsterAPI() {
        return usesHamsterAPI;
    }

    public boolean usesNexEngine() {
        return usesNexEngine;
    }

    public boolean usesCustomDisplay() {
        return usesCustomDisplay;
    }
}
