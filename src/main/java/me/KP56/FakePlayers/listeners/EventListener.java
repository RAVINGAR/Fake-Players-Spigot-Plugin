package me.KP56.FakePlayers.listeners;

import me.KP56.FakePlayers.FakePlayer;
import me.KP56.FakePlayers.FakePlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.server.ServerLoadEvent;

import java.util.logging.Level;

public class EventListener implements Listener {
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        FakePlayer fakePlayer = FakePlayer.getFakePlayer(e.getEntity().getUniqueId());
        if (fakePlayer != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(FakePlugin.getPlugin(), fakePlayer::respawn, 100);
        }
    }

    /*
    @EventHandler(priority = EventPriority.HIGHEST)
    public void preLoginListener(PlayerPreLoginEvent e) {
        FakePlayer player = FakePlayer.getFakePlayer(e.getName());
        if (player != null) {
            player.removePlayer();
        };
    }*/
}
