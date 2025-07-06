package com.sirmaku.customFishing;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import net.kyori.adventure.text.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class LootCommand implements CommandExecutor {

    private final CustomFishing plugin = CustomFishing.getInstance();
    private final YamlConfiguration cfg;
    private final File lootFile;

    public LootCommand() {
        lootFile = new File(plugin.getDataFolder(), "loot.yml");
        cfg = YamlConfiguration.loadConfiguration(lootFile);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {

        if (!s.hasPermission("customfishing.admin")) {
            s.sendMessage(Component.text("Bạn không có quyền.").color(NamedTextColor.RED));
            return true;
        }
        if (a.length == 0) { sendHelp(s); return true; }

        /* ----- hỗ trợ cú pháp `/cf loot <sub> …` ----- */
        int idx = 0;
        if (a[0].equalsIgnoreCase("loot")) {
            if (a.length == 1) { sendHelp(s); return true; }
            idx = 1;                   // bỏ qua từ “loot”
        }

        String sub = a[idx].toLowerCase();
        String[] args = Arrays.copyOfRange(a, idx, a.length);   // shift mảng

        switch (sub) {
            case "list"       -> doList(s);
            case "chance"     -> doChance(s, args);
            case "durability" -> doDurability(s, args);
            case "additem"    -> doAddItem(s, args);
            case "delitem"    -> doDelItem(s, args);
            case "save"       -> doSave(s);
            case "reload"     -> {
                LootManager.loadLoot();
                s.sendMessage(Component.text("Đã reload bảng loot.").color(NamedTextColor.GREEN));
            }
            default -> sendHelp(s);
        }
        return true;
    }

    /* ---------- sub‑commands ---------- */

    private void doList(CommandSender s) {
        ConfigurationSection loot = cfg.getConfigurationSection("loot");
        if (loot == null) { s.sendMessage("§cKhông có loot!"); return; }
        loot.getKeys(false).forEach(t -> {
            int ch = loot.getInt(t + ".chance");
            List<?> items = loot.getMapList(t + ".items");
            s.sendMessage("§e" + t + "§7: chance §a" + ch + "%§7, items " + items.size());
        });
    }

    private void doChance(CommandSender s, String[] a) {
        if (a.length != 3) { s.sendMessage("/cf loot chance <tier> <percent>"); return; }
        String tier = a[1];
        int pct;
        try { pct = Integer.parseInt(a[2]); }
        catch (NumberFormatException e) { s.sendMessage("§cPhần trăm phải là số."); return; }

        cfg.set("loot." + tier + ".chance", pct);
        s.sendMessage("§aĐã đặt chance " + tier + " = " + pct + "%");
    }

    private void doDurability(CommandSender s, String[] a) {
        if (a.length != 3) { s.sendMessage("/cf loot durability <tier> <loss>"); return; }
        String tier = a[1];
        int loss;
        try { loss = Integer.parseInt(a[2]); }
        catch (NumberFormatException e) { s.sendMessage("§cSố phải là int."); return; }
        cfg.set("loot." + tier + ".durability_loss", loss);
        s.sendMessage("§aĐã đặt durability_loss " + tier + " = " + loss);
    }

    private void doAddItem(CommandSender s, String[] a) {
        if (a.length < 3) { s.sendMessage("/cf loot additem <tier> <type> [amount]"); return; }
        String tier = a[1];
        String type = a[2];
        int amt = (a.length >= 4) ? Integer.parseInt(a[3]) : 1;

        Map<String, Object> map = Map.of("type", type, "amount", amt);
        List<Map<?,?>> list = cfg.getMapList("loot." + tier + ".items");
        list.add(map);
        cfg.set("loot." + tier + ".items", list);

        s.sendMessage("§aĐã thêm " + type + " x" + amt + " vào tier " + tier);
    }

    private void doDelItem(CommandSender s, String[] a) {
        if (a.length != 3) { s.sendMessage("/cf loot delitem <tier> <index>"); return; }
        String tier = a[1];
        int idx = Integer.parseInt(a[2]);

        List<Map<?,?>> list = cfg.getMapList("loot." + tier + ".items");
        if (idx < 0 || idx >= list.size()) {
            s.sendMessage("§cIndex ngoài phạm vi, 0–" + (list.size()-1));
            return;
        }
        list.remove(idx);
        cfg.set("loot." + tier + ".items", list);
        s.sendMessage("§aĐã xoá item #" + idx + " của tier " + tier);
    }

    private void doSave(CommandSender s) {
        try {
            LootManager.saveLoot(cfg); // Ghi file + chuẩn hóa
            s.sendMessage("§aĐã lưu & reload loot.yml.");
        } catch (Exception ex) {
            s.sendMessage("§cLưu thất bại: " + ex.getMessage());
        }
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§6/cf loot list");
        s.sendMessage("§6/cf loot chance <tier> <percent>");
        s.sendMessage("§6/cf loot durability <tier> <loss>");
        s.sendMessage("§6/cf loot additem <tier> <type> [amount]");
        s.sendMessage("§6/cf loot delitem <tier> <index>");
        s.sendMessage("§6/cf loot save  §7- Lưu file & reload");
        s.sendMessage("§6/cf loot reload §7- Chỉ reload (huỷ thay đổi chưa save)");
    }
}
