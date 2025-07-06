package com.sirmaku.customFishing;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.util.Random;

public class BarMinigameSession {

    private static final Random RND = new Random();

    private final Player player;
    private final FishHook hook;

    private final int totalBars;
    private final int barLength = GameConfig.getBarLength();

    private int currentBar = 0;
    private int pos;
    private int safeStart, safeEnd;
    private boolean movingRight;

    private int timeLeft;             // tick đếm ngược

    public BarMinigameSession(Player player, FishHook hook, int totalBars) {
        this.player = player;
        this.hook   = hook;
        this.totalBars = totalBars;

        this.timeLeft = GameConfig.randomTimeTicks();
        initNewBar();
    }

    /* ===== getters ===== */
    public Player getPlayer()        { return player; }
    public FishHook getHook()        { return hook; }
    public int getBarLength()        { return barLength; }
    public int getPosition()         { return pos; }
    public int getSafeStart()        { return safeStart; }
    public int getSafeEnd()          { return safeEnd; }
    public int getTickRate()         { return GameConfig.getTickRate(); }
    public int getCurrentBar()       { return currentBar; }
    public int getTotalBars()        { return totalBars; }
    public int getTimeLeftTicks()    { return timeLeft; }

    /* ===== logic ===== */
    public void tick() {             // gọi mỗi vòng
        step();
        timeLeft--;
    }

    public void step() {
        pos += movingRight ? 1 : -1;
        if (pos <= 0)                  { pos = 0;               movingRight = true;  }
        else if (pos >= barLength - 1) { pos = barLength - 1;   movingRight = false; }
    }

    /** true nếu nhấn trong vùng xanh */
    public boolean handleClick() {
        return isInSafeZone();
    }

    public boolean isInSafeZone() {
        return pos >= safeStart && pos < safeEnd;
    }

    /** true nếu vượt xong */
    public boolean nextBar() {
        currentBar++;
        if (currentBar >= totalBars) return true;
        initNewBar();
        return false;
    }

    private void initNewBar() {
        int safeWidth = GameConfig.safeWidth(totalBars);
        safeStart = RND.nextInt(barLength - safeWidth);
        safeEnd   = safeStart + safeWidth;
        pos         = RND.nextInt(barLength);
        movingRight = RND.nextBoolean();
    }
}
