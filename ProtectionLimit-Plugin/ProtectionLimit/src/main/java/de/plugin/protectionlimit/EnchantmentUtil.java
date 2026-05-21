package de.plugin.protectionlimit;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility methods for detecting and downgrading Protection enchantments.
 */
public final class EnchantmentUtil {

    private EnchantmentUtil() {}

    /**
     * Checks whether an item has a Protection enchantment above the given max level,
     * and downgrades it if so.
     *
     * @param item     the item to check (may be null / AIR)
     * @param maxLevel the maximum allowed Protection level
     * @return true if the item was modified
     */
    public static boolean downgradeItem(ItemStack item, int maxLevel) {
        if (item == null || item.getType().isAir()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        boolean changed = false;

        // --- Enchanted books: stored enchantments ---
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            int storedLevel = storageMeta.getStoredEnchantLevel(Enchantment.PROTECTION);
            if (storedLevel > maxLevel) {
                storageMeta.removeStoredEnchant(Enchantment.PROTECTION);
                storageMeta.addStoredEnchant(Enchantment.PROTECTION, maxLevel, true);
                item.setItemMeta(storageMeta);
                changed = true;
            }
            return changed;
        }

        // --- Armor / other items: regular enchantments ---
        int level = meta.getEnchantLevel(Enchantment.PROTECTION);
        if (level > maxLevel) {
            meta.removeEnchant(Enchantment.PROTECTION);
            meta.addEnchant(Enchantment.PROTECTION, maxLevel, true);
            item.setItemMeta(meta);
            changed = true;
        }

        return changed;
    }

    /**
     * Iterates over every slot in an inventory and downgrades over-limit Protection.
     *
     * @param inventory the inventory to scan
     * @param maxLevel  the maximum allowed Protection level
     * @return number of items that were modified
     */
    public static int downgradeInventory(Inventory inventory, int maxLevel) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (downgradeItem(item, maxLevel)) count++;
        }
        return count;
    }
}
