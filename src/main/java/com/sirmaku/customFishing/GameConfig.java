package com.sirmaku.customFishing;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Random;

/** Đọc & giữ cấu hình – 4 độ khó: EASY / NORMAL / HARD / EXTREME */
public final class GameConfig {

    /* ===== trường cấu hình ===== */
    private static int minBars, maxBars;
    private static int barLength, tickRate;

    private static int thresholdEasy, thresholdNormal, thresholdHard;
    private static int safeEasy, safeNormal, safeHard;

    private static int minTimeSec, maxTimeSec;

    private static int penaltyEasy, penaltyNormal, penaltyHard, penaltyExtreme;

    private static final Random RND = new Random();
    private GameConfig() {}

    /* ===== load từ plugins/CustomFishing/config.yml ===== */
    public static void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) plugin.saveResource("config.yml", false);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        minBars   = cfg.getInt("min_bars", 3);
        maxBars   = cfg.getInt("max_bars", 20);
        barLength = cfg.getInt("bar_length", 25);
        tickRate  = cfg.getInt("tick_rate", 2);

        thresholdEasy   = cfg.getInt("thresholds.easy", 5);
        thresholdNormal = cfg.getInt("thresholds.normal", 12);
        thresholdHard   = cfg.getInt("thresholds.hard", 20);   // mới

        safeEasy   = cfg.getInt("safe_width.easy", 7);
        safeNormal = cfg.getInt("safe_width.normal", 5);
        safeHard   = cfg.getInt("safe_width.hard", 4);

        minTimeSec = cfg.getInt("min_time_sec", 3);
        maxTimeSec = cfg.getInt("max_time_sec", 12);

        penaltyEasy    = cfg.getInt("fail_penalty.easy",    2);
        penaltyNormal  = cfg.getInt("fail_penalty.normal",  4);
        penaltyHard    = cfg.getInt("fail_penalty.hard",    6);
        penaltyExtreme = cfg.getInt("fail_penalty.extreme", 8); // mới
    }

    /* ===== tiện ích random ===== */
    public static int randomBars() {
        return RND.nextInt(maxBars - minBars + 1) + minBars;
    }
    public static int randomTimeTicks() {
        int sec = RND.nextInt(maxTimeSec - minTimeSec + 1) + minTimeSec;
        return sec * 20;
    }

    /* ===== phân cấp độ khó & vùng an toàn ===== */
    public static Difficulty diffOf(int bars) {
        if (bars <= thresholdEasy)            return Difficulty.EASY;
        if (bars <= thresholdNormal)          return Difficulty.NORMAL;
        if (bars <= thresholdHard)            return Difficulty.HARD;
        return Difficulty.EXTREME;
    }
    public static int safeWidth(int bars) {
        return switch (diffOf(bars)) {
            case EASY    -> safeEasy;
            case NORMAL  -> safeNormal;
            case HARD,
                 EXTREME -> safeHard;       // dùng cùng một width cho hard/ extreme
        };
    }

    /* ===== penalty độ bền khi THUA ===== */
    public static int penaltyByDifficulty(Difficulty d) {
        return switch (d) {
            case EASY    -> penaltyEasy;
            case NORMAL  -> penaltyNormal;
            case HARD    -> penaltyHard;
            case EXTREME -> penaltyExtreme;
        };
    }

    /* ===== getter cần dùng ===== */
    public static int getBarLength() { return barLength; }
    public static int getTickRate()  { return tickRate; }

    /* ===== enum ===== */
    public enum Difficulty { EASY, NORMAL, HARD, EXTREME }
}
