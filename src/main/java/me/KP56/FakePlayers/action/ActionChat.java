package me.KP56.FakePlayers.action;

import me.KP56.FakePlayers.FakePlayer;
import me.KP56.FakePlayers.FakePlugin;
import me.KP56.FakePlayers.socket.FakePlayersSocket;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class ActionChat implements Action {

    private String message;

    public ActionChat(String message) {
        this.message = message;
    }

    @Override
    public void perform(FakePlayer player) {
        if(FakePlugin.getPlugin().config.getBoolean("bungeecord.enabled")) {
            FakePlayersSocket.fakePlayersSocket.send(FakePlugin.getPlugin().config.getString("bungeecord.ip"), FakePlugin.getPlugin().config.getInt("bungeecord.bungeecord-fakeplayers-port"), "chat " + player.getName() + " " + message);
        }
        player.getEntityPlayer().getBukkitEntity().chat(message);
    }

    @Override
    public ActionType getType() {
        return ActionType.CHAT;
    }

    public String getMessage() {
        return message;
    }
}
