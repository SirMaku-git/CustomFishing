package com.sirmaku.customFishing;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/**
 * Main‑class của plugin CustomFishing.
 *  • Nạp config & loot
 *  • Đăng ký resolver mặc định IA / Oraxen / MMOItems / MoreFish *bằng reflection* (không cần dep)
 *  • Lệnh /customfishing reload
 */
public class CustomFishing extends JavaPlugin {

    private static CustomFishing instance;
    public static CustomFishing getInstance() { return instance; }

    private Logger LOG;

    @Override
    public void onEnable() {
        instance = this;
        LOG = getLogger();

        saveResource("config.yml", false);
        saveResource("loot.yml",   false);

        GameConfig.load(this);
        registerDefaultResolvers();
        LootManager.loadLoot();

        Bukkit.getPluginManager().registerEvents(new FishingHookListener(), this);
        Bukkit.getPluginManager().registerEvents(new BarMinigameListener(), this);

        PluginCommand cmd = getCommand("cf");
        if (cmd != null) {
            cmd.setExecutor(new LootCommand());
        } else {
            getLogger().warning("Lệnh 'cf' không được khai báo trong plugin.yml!");
        }

        LOG.info("CustomFishing enabled!");
    }

    /* ===== /customfishing reload ===== */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof Player p && !p.hasPermission("customfishing.admin")) {
                p.sendMessage("§cBạn không có quyền.");
                return true;
            }
            GameConfig.load(this);
            LootManager.loadLoot();
            sender.sendMessage("§a[CustomFishing] Reloaded config & loot.");
            return true;
        }
        sender.sendMessage("§eUsage: /customfishing reload");
        return true;
    }

    /* ===== register resolvers bằng reflection ===== */
    private void registerDefaultResolvers() {

        /* ItemsAdder ➜ ia:ID */
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            CustomFishingAPI.registerResolver("ia", makeReflectResolver(
                    "dev.lone.itemsadder.api.CustomStack",
                    "getInstance",            // static String → CustomStack
                    "getItemStack"));         // instance → ItemStack
            LOG.info("Hooked ItemsAdder (ia:)");
        }

        /* Oraxen ➜ orx:ID */
        if (Bukkit.getPluginManager().isPluginEnabled("Oraxen")) {
            CustomFishingAPI.registerResolver("orx", id -> {
                try {
                    Class<?> cls = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                    Object itemBuilder = cls.getMethod("getItemById", String.class).invoke(null, id);
                    if (itemBuilder == null) return null;
                    return (ItemStack) itemBuilder.getClass().getMethod("build").invoke(itemBuilder);
                } catch (Throwable t) { return null; }
            });
            LOG.info("Hooked Oraxen (orx:)");
        }

        /* MMOItems ➜ mmo:TYPE:ID */
        if (Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            CustomFishingAPI.registerResolver("mmo", raw -> {
                try {
                    String[] split = raw.split(":", 2); // TYPE:ID
                    if (split.length != 2) return null;
                    Class<?> main = Class.forName("net.Indyuce.mmoitems.MMOItems");
                    Object plugin = main.getField("plugin").get(null);
                    return (ItemStack) plugin.getClass()
                            .getMethod("getItem", String.class, String.class)
                            .invoke(plugin, split[0], split[1]);
                } catch (Throwable t) { return null; }
            });
            LOG.info("Hooked MMOItems (mmo:)");
        }

        /* MoreFish ➜ morefish:ID */
        if (Bukkit.getPluginManager().isPluginEnabled("MoreFish")) {
            CustomFishingAPI.registerResolver("morefish", id -> {
                try {
                    Class<?> mfMain = Class.forName("me.elsiff.morefish.MoreFish");
                    Object plugin = mfMain.getMethod("getPlugin").invoke(null);
                    Object manager = plugin.getClass().getMethod("getFishesManager").invoke(plugin);
                    Object fishType = manager.getClass().getMethod("getFishType", String.class).invoke(manager, id);
                    if (fishType == null) return null;
                    return (ItemStack) fishType.getClass().getMethod("buildItemStack").invoke(fishType);
                } catch (Throwable t) { return null; }
            });
            LOG.info("Hooked MoreFish (morefish:)");
        }
    }

    /* helper tạo resolver với 1 method static + 1 method instance */
    private Function<String, ItemStack> makeReflectResolver(String className,
                                                            String staticMethod, String stackMethod) {
        return id -> {
            try {
                Class<?> cls = Class.forName(className);
                Object obj = cls.getMethod(staticMethod, String.class).invoke(null, id);
                if (obj == null) return null;
                return (ItemStack) obj.getClass().getMethod(stackMethod).invoke(obj);
            } catch (Throwable t) { return null; }
        };
    }
}
