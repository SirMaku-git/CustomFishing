package com.sirmaku.customFishing;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

public class BarMinigameListener implements Listener {

    @EventHandler
    public void onLeftClick(PlayerInteractEvent e) {
        // CHỈ nhận click trái
        Action act = e.getAction();
        if (act != Action.LEFT_CLICK_AIR && act != Action.LEFT_CLICK_BLOCK) return;

        if (BarMinigameManager.isPlaying(e.getPlayer())) {
            e.setCancelled(true);          // không kích hoạt animation đánh
            BarMinigameManager.handleClick(e.getPlayer());
        }
    }
}
