package com.sirmaku.customFishing;

import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Public API cho phép plugin bên ngoài đăng ký resolver <namespace> → ItemStack.
 *
 * <pre>
 * CustomFishingAPI.registerResolver("ia", id -> ItemsAdder.getCustomItem(id));
 * </pre>
 */
public final class CustomFishingAPI {

    private CustomFishingAPI() {}

    /* Map lưu resolver theo namespace (lowercase) */
    private static final Map<String, Function<String, ItemStack>> RESOLVERS = new ConcurrentHashMap<>();

    /**
     * Đăng ký (hoặc ghi đè) resolver.
     * @param namespace  tiền tố trước dấu ':' trong loot.yml
     * @param resolver   hàm nhận id → trả ItemStack (hoặc null nếu id không hợp lệ)
     */
    public static void registerResolver(String namespace, Function<String, ItemStack> resolver) {
        if (namespace == null || resolver == null) return;
        RESOLVERS.put(namespace.toLowerCase(), resolver);
    }

    /**
     * Lấy resolver cho nội bộ LootManager.
     * @param namespace tiền tố (không phân biệt hoa thường)
     * @return Function hoặc null nếu chưa đăng ký
     */
    static Function<String, ItemStack> getResolver(String namespace) {
        return RESOLVERS.get(namespace.toLowerCase());
    }
}
