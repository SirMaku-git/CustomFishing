package com.sirmaku.customFishing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class BarMinigameManager {

    private static final Map<UUID, BarMinigameSession> SESSIONS = new HashMap<>();
    private static final Random RND = new Random();

    /* ===== khởi tạo mini-game ===== */
    public static void startGame(Player p, FishHook hook) {
        if (isPlaying(p)) return;

        int bars = GameConfig.randomBars();
        GameConfig.Difficulty diff = GameConfig.diffOf(bars);

        BarMinigameSession s = new BarMinigameSession(p, hook, bars);
        SESSIONS.put(p.getUniqueId(), s);

        p.showTitle(Title.title(
                Component.text("🎣 Cá cắn câu! (" + diffName(diff) + ")", NamedTextColor.YELLOW),
                Component.text("Cần kéo " + bars + " lần • Chờ ⚡ rồi BẤM trái", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(300))
        ));
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        runLoop(s);
    }

    public static void handleClick(Player p) {
        BarMinigameSession s = SESSIONS.get(p.getUniqueId());
        if (s == null) return;

        boolean ok = s.handleClick();
        p.playSound(p, Sound.UI_BUTTON_CLICK, 1f, ok ? 1.8f : 0.5f);

        if (!ok) { endGame(p, s, false); return; }

        boolean done = s.nextBar();
        if (done) {
            endGame(p, s, true);
        } else {
            p.showTitle(Title.title(
                    Component.text("✔ " + s.getCurrentBar() + "/" + s.getTotalBars(), NamedTextColor.AQUA),
                    Component.text("Độ khó: " + diffName(GameConfig.diffOf(s.getTotalBars())), NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(400), Duration.ofMillis(100))
            ));
        }
    }

    public static boolean isPlaying(Player p) { return SESSIONS.containsKey(p.getUniqueId()); }

    /* ===== vòng lặp ===== */
    private static void runLoop(BarMinigameSession s) {
        new BukkitRunnable() {
            @Override public void run() {
                Player p = s.getPlayer();
                if (!p.isOnline() || !isPlaying(p)) { cancel(); return; }

                s.tick();                       // di chuyển + giảm thời gian

                if (s.getTimeLeftTicks() <= 0) {
                    boolean win = s.isInSafeZone();  // timeout: thắng nếu trong xanh
                    endGame(p, s, win);
                    cancel();
                    return;
                }

                p.sendActionBar(
                        LegacyComponentSerializer.legacySection()
                                .deserialize(renderActionBar(s))
                );
            }
        }.runTaskTimer(CustomFishing.getInstance(), 0, s.getTickRate());
    }

    /* ===== render action-bar 2 dòng ===== */
    private static String renderActionBar(BarMinigameSession s) {
        String time = "§e⌛ §f" + String.format("%.1f", s.getTimeLeftTicks() / 20.0) + "s";
        return time + "\n" + renderBar(s);
    }

    private static String renderBar(BarMinigameSession s) {
        StringBuilder sb = new StringBuilder("§e⚡ §6[");
        for (int i = 0; i < s.getBarLength(); i++) {
            if (i == s.getPosition())                   sb.append("§e⚡");
            else if (i >= s.getSafeStart() && i < s.getSafeEnd()) sb.append("§a▉");
            else                                        sb.append("§c▉");
        }
        sb.append("§6] §f").append(s.getCurrentBar() + 1).append('/').append(s.getTotalBars());
        return sb.toString();
    }

    public static void forceFail(Player p) {
        BarMinigameSession s = SESSIONS.get(p.getUniqueId());
        if (s != null) endGame(p, s, false);
    }
    /* ===== kết thúc ===== */
    private static void endGame(Player p, BarMinigameSession s, boolean win) {

        /* thu hook */
        if (s.getHook() != null && !s.getHook().isDead()) s.getHook().remove();

        /* giảm độ bền khi fail */
        if (!win) damageRodFail(p, s.getTotalBars());

        /* hiệu ứng & loot */
        if (win) {
            p.showTitle(Title.title(
                    Component.text("✅ Thành công!", NamedTextColor.GREEN),
                    Component.text("Bạn bắt được cá!", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(300))
            ));
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        } else {
            p.showTitle(Title.title(
                    Component.text("❌ Thất bại!", NamedTextColor.RED),
                    Component.text("Cá tuột mất…", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(300))
            ));
            p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
        }

        LootManager.giveLoot(p, win, s.getTotalBars());
        SESSIONS.remove(p.getUniqueId());
    }

    /* ===== helper ===== */
    private static void damageRodFail(Player p, int bars) {
        GameConfig.Difficulty diff = GameConfig.diffOf(bars);
        int dmg = GameConfig.penaltyByDifficulty(diff) * bars;
        ItemStack rod = p.getInventory().getItemInMainHand();
        if (rod.getType() != Material.FISHING_ROD) return;

        ItemMeta meta = rod.getItemMeta();
        if (meta instanceof Damageable dm) {
            dm.setDamage(dm.getDamage() + dmg);
            rod.setItemMeta(meta);
        }
    }

    private static String diffName(GameConfig.Difficulty d) {
        return switch (d) {
            case EASY     -> "§aDễ";
            case NORMAL   -> "§eThường";
            case HARD     -> "§cKhó";
            case EXTREME  -> "§4Cực Khó";   // màu đỏ đậm
        };
    }
}
