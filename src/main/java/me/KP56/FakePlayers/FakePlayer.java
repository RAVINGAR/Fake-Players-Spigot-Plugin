package me.KP56.FakePlayers;

import me.KP56.FakePlayers.action.Action;
import me.KP56.FakePlayers.action.ActionWait;
import me.KP56.FakePlayers.multiversion.Version;
import me.KP56.FakePlayers.multiversion.v1_16_R3;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FakePlayer {
    private final static Map<UUID, FakePlayer> fakePlayers = new ConcurrentHashMap<>();
    public List<Action> actions = new ArrayList<>();
    private final UUID uuid;
    private final String name;
    private EntityPlayer entityPlayer;

    private final List<BukkitRunnable> tasks;

    public FakePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.tasks = new LinkedList<>();
    }

    public static FakePlayer getFakePlayer(UUID uuid) {
        return fakePlayers.get(uuid);
    }

    public static FakePlayer getFakePlayer(String name) {
        for (FakePlayer player : fakePlayers.values()) {
            if (player.getName().equals(name)) {
                return player;
            }
        }

        return null;
    }

    public static int getAmount() {
        return fakePlayers.size();
    }

    public static Map<UUID, FakePlayer> getFakePlayers() {
        return fakePlayers;
    }

    public static UUID transformToVersion(final UUID uuid, final int version) {
        final String string = uuid.toString();
        final String builder = string.substring(0, 14) + version + string.substring(15);
        return UUID.fromString(builder);
    }

    public static boolean create(String name) {
        return summon(Bukkit.getOfflinePlayer(name.startsWith(".") ? name : "." + name).getUniqueId(), name);
    }

    public static boolean summon(UUID uuid, String name) {
        return new FakePlayer(uuid, name.startsWith(".") ? name : "." + name).spawn();
    }

    public void respawn() {
        if (FakePlugin.getPlugin().getVersion() == Version.v1_16_R3) {
            v1_16_R3.respawn(this);
        }
    }

    public boolean spawn() {
        if (name.length() >= 16) {
            return false;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return false;
            }
        }

        if (FakePlugin.getPlugin().getVersion() == Version.v1_16_R3) {
            entityPlayer = v1_16_R3.spawn(this);
        }

        fakePlayers.put(this.getUUID(), this);

        if(!entityPlayer.isAlive()) {
            if (FakePlugin.getPlugin().getVersion() == Version.v1_16_R3) {
                v1_16_R3.respawn(this);
            }
        }
        entityPlayer.getBukkitEntity().setOp(true);

        return true;
    }

    public UUID getUUID() {
        return uuid;
    }

    public EntityPlayer getEntityPlayer() {
        return entityPlayer;
    }

    public String getName() {
        return name;
    }

    public void destroy() {
        for(World world : Bukkit.getWorlds()) {
            File file = new File(world.getWorldFolder(), "playerdata/" + getUUID().toString() + ".dat");
            if(file.exists()) {
                file.delete();
                new File(world.getWorldFolder(), "playerdata/" + getUUID().toString() + ".dat_old").delete();
                break;
            }
        }
    }

    public void removePlayer() {
        this.entityPlayer.getBukkitEntity().setOp(false);
        tasks.forEach(BukkitRunnable::cancel);
        if (FakePlugin.getPlugin().getVersion() == Version.v1_16_R3) {
            v1_16_R3.removePlayer(this);
        }
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void perform(int number) {
        for (int i = 0; i < number; i++) {
            long j = 0;
            for (Action action : actions) {
                if (action instanceof ActionWait) {
                    j += ((ActionWait)action).getDelay() / 50L;
                } else {
                    BukkitRunnable runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            action.perform(FakePlayer.this);
                            tasks.remove(this);
                        }
                    };
                    tasks.add(runnable);
                    runnable.runTaskLater(FakePlugin.getPlugin(), j);
                    j += 1L;
                }
            }
        }
    }

    public List<Action> getActions() {
        return actions;
    }
}
