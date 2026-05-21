package de.plugin.protectionlimit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Listens for all situations in which a Prot IV item could enter the game
 * and immediately downgrades it to the configured maximum level.
 */
public class ProtectionListener implements Listener {

    private final JavaPlugin plugin;
    private final int maxLevel;

    public ProtectionListener(JavaPlugin plugin, int maxLevel) {
        this.plugin = plugin;
        this.maxLevel = maxLevel;
    }

    /**
     * Starts a repeating task (every 5 seconds) that scans:
     * - All online players' inventories (including equipment)
     * - All loaded container blocks in every loaded chunk (chests, hoppers, etc.)
     * - All item entities on the ground
     */
    public void startPeriodicScan() {
        // Run every 100 ticks = 5 seconds
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int total = 0;

            // 1. Player inventories
            for (Player player : Bukkit.getOnlinePlayers()) {
                total += EnchantmentUtil.downgradeInventory(player.getInventory(), maxLevel);
            }

            // 2. Loaded container blocks (chests, barrels, hoppers, dispensers, etc.)
            for (World world : Bukkit.getWorlds()) {
                for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state instanceof Container container) {
                            total += EnchantmentUtil.downgradeInventory(container.getInventory(), maxLevel);
                        }
                    }
                }

                // 3. Item entities lying on the ground
                for (Item itemEntity : world.getEntitiesByClass(Item.class)) {
                    if (EnchantmentUtil.downgradeItem(itemEntity.getItemStack(), maxLevel)) {
                        itemEntity.setItemStack(itemEntity.getItemStack());
                        total++;
                    }
                }
            }

            if (total > 0) {
                plugin.getLogger().info("[PeriodicScan] Downgraded " + total + " item(s) to Prot III.");
            }
        }, 100L, 100L); // delay 5s, repeat every 5s
    }

    // -----------------------------------------------------------------------
    // Anvil: combining two items or applying a book
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) return;

        if (EnchantmentUtil.downgradeItem(result, maxLevel)) {
            event.setResult(result);
        }
    }

    // -----------------------------------------------------------------------
    // Enchanting table
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchantItem(EnchantItemEvent event) {
        // Schedule one tick later – the item hasn't received its enchants yet
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack item = event.getItem();
            if (EnchantmentUtil.downgradeItem(item, maxLevel)) {
                Player player = event.getEnchanter();
                player.sendMessage("§eProtectionLimit: §fProtection downgraded to Prot III.");
            }
        });
    }

    // -----------------------------------------------------------------------
    // Player joins – scan their whole inventory (e.g. items from another server)
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int fixed = EnchantmentUtil.downgradeInventory(player.getInventory(), maxLevel);
        if (fixed > 0) {
            plugin.getLogger().info("Downgraded " + fixed + " item(s) in " + player.getName() + "'s inventory.");
            player.sendMessage("§eProtectionLimit: §f" + fixed + " item(s) were downgraded to Prot III.");
        }
    }

    // -----------------------------------------------------------------------
    // Item pickup – downgrade the moment a player picks up an item
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (EnchantmentUtil.downgradeItem(item, maxLevel)) {
            event.getItem().setItemStack(item);
        }
    }

    // -----------------------------------------------------------------------
    // Inventory clicks – covers trading, chests, hopper transfers via GUI, etc.
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        EnchantmentUtil.downgradeItem(cursor, maxLevel);
        EnchantmentUtil.downgradeItem(current, maxLevel);
    }

    // -----------------------------------------------------------------------
    // Inventory close – final safety net: scan the whole inventory on close
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        EnchantmentUtil.downgradeInventory(player.getInventory(), maxLevel);

        // Also check the inventory that was closed (e.g. a chest)
        if (!(event.getInventory() instanceof AnvilInventory)) {
            EnchantmentUtil.downgradeInventory(event.getInventory(), maxLevel);
        }
    }
}
