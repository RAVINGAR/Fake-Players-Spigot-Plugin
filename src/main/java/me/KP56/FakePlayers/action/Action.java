package me.KP56.FakePlayers.action;

import me.KP56.FakePlayers.FakePlayer;

public interface Action {
    void perform(FakePlayer player);
    ActionType getType();
}
