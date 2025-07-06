package com.sirmaku.customFishing;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * LootManager — linh động: hỗ trợ
 *   • Vanilla Material  ("DIAMOND")
 *   • <namespace>:<id> qua CustomFishingAPI (ia, orx, mmo, morefish, ...)
 *   • Serialized item: dạng YAML (an toàn, không deprecated)
 */
public final class LootManager {

    private LootManager() {}

    private static final Random RND = new Random();
    private static final List<LootEntry> LOOT_TABLE = new ArrayList<>();
    private static final Logger LOG = CustomFishing.getInstance().getLogger();

    /* đường dẫn file loot.yml ngoài plugin‑folder */
    private static final File LOOT_FILE =
            new File(CustomFishing.getInstance().getDataFolder(), "loot.yml");

    /* =================== LOAD =================== */
    public static void loadLoot() {
        LOOT_TABLE.clear();

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(LOOT_FILE);
        ConfigurationSection lootSec = cfg.getConfigurationSection("loot");
        if (lootSec == null) {
            LOG.warning("loot.yml không có section 'loot'");
            return;
        }

        for (String tier : lootSec.getKeys(false)) {
            ConfigurationSection sec = lootSec.getConfigurationSection(tier);
            if (sec == null) continue;

            int chance = sec.getInt("chance", 0);
            int loss   = sec.getInt("durability_loss", 1);

            for (Map<?, ?> raw : sec.getMapList("items")) {
                Object typeObj = raw.get("type");
                ItemStack item = resolveItem(typeObj);
                if (item == null) {
                    LOG.warning("Unknown item in loot.yml: " + typeObj);
                    continue;
                }
                int amt = raw.get("amount") instanceof Number n ? n.intValue() : 1;
                item.setAmount(amt);

                LOOT_TABLE.add(new LootEntry(chance, loss, item));
            }
        }
        LOG.info("Loaded " + LOOT_TABLE.size() + " loot entries.");
    }

    /* =================== SAVE (cho /cf loot save) =================== */
    public static void saveLoot(YamlConfiguration cfg) throws IOException {
        cfg.save(LOOT_FILE);
        loadLoot();          // nạp lại vào bộ nhớ
    }

    /* ====== RESOLVE item theo <namespace>:<id>, YAML map, hoặc Material ====== */
    private static ItemStack resolveItem(Object typeObj) {
        /* YAML‑ItemStack dạng map */
        if (typeObj instanceof Map<?,?> map) {
            try {
                ConfigurationSection sec = new YamlConfiguration();
                for (Map.Entry<?,?> e : map.entrySet()) {
                    sec.set(e.getKey().toString(), e.getValue());
                }
                return ItemStack.deserialize(sec.getValues(false));
            } catch (Exception ignore) {}
        }

        /* String identifier */
        if (typeObj instanceof String name) {
            if (name.contains(":")) {
                /* namespace:id */
                String[] split = name.split(":", 2);
                String ns = split[0];
                String id = split[1];

                var fn = CustomFishingAPI.getResolver(ns);
                if (fn != null) {
                    try {
                        ItemStack it = fn.apply(id);
                        if (it != null) return it.clone();
                    } catch (Exception ignore) {}
                }
            }
            /* Vanilla material */
            Material mat = Material.matchMaterial(name.toUpperCase());
            return (mat != null) ? new ItemStack(mat) : null;
        }
        return null;
    }

    /* =================== GIVE LOOT =================== */
    public static void giveLoot(Player p, boolean success, int bars) {
        if (!success) {
            damageRod(p, GameConfig.penaltyByDifficulty(GameConfig.diffOf(bars)));
            return;
        }
        Collections.shuffle(LOOT_TABLE, RND);
        for (LootEntry e : LOOT_TABLE) {
            if (RND.nextInt(100) < e.chance()) {
                p.getInventory().addItem(e.item().clone());
                damageRod(p, e.durabilityLoss());
                return;
            }
        }
        /* hiếm khi không trúng gì */
        damageRod(p, 1);
    }

    /* =================== utils =================== */
    private static void damageRod(Player p, int dmg) {
        ItemStack rod = p.getInventory().getItemInMainHand();
        if (rod.getType() != Material.FISHING_ROD) return;

        ItemMeta meta = rod.getItemMeta();
        if (meta instanceof Damageable d) {
            d.setDamage(d.getDamage() + dmg);
            rod.setItemMeta(meta);
        }
    }
}