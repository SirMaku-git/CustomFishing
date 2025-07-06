package com.sirmaku.customFishing;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class FishingHookListener implements Listener {

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        FishHook hook = e.getHook();

        switch (e.getState()) {
            case BITE -> {                              // cá cắn
                if (!BarMinigameManager.isPlaying(p)) {
                    e.setCancelled(true);
                    BarMinigameManager.startGame(p, hook);
                }
            }
            case CAUGHT_FISH, CAUGHT_ENTITY -> {        // tránh loot vanilla
                if (BarMinigameManager.isPlaying(p)) e.setCancelled(true);
            }
            case FISHING -> {                           // người chơi VỪA quăng dây
                if (BarMinigameManager.isPlaying(p)) {
                    // fail ngay lập tức, gãy cần (fail ⇒ trừ độ bền)
                    BarMinigameManager.forceFail(p);
                }
            }
            default -> {}
        }
    }
}
