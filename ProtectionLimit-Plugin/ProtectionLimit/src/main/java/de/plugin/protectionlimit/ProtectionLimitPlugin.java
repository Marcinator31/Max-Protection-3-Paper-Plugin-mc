package de.plugin.protectionlimit;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ProtectionLimitPlugin extends JavaPlugin implements CommandExecutor {

    private static final int MAX_PROTECTION_LEVEL = 3;

    @Override
    public void onEnable() {
        ProtectionListener listener = new ProtectionListener(this, MAX_PROTECTION_LEVEL);
        getServer().getPluginManager().registerEvents(listener, this);
        listener.startPeriodicScan();

        var cmd = getCommand("protectionlimit");
        if (cmd != null) cmd.setExecutor(this);

        getLogger().info("ProtectionLimit enabled – max Protection level set to " + MAX_PROTECTION_LEVEL + ".");
    }

    @Override
    public void onDisable() {
        getLogger().info("ProtectionLimit disabled.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("fix")) {
            int totalFixed = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                int fixed = EnchantmentUtil.downgradeInventory(player.getInventory(), MAX_PROTECTION_LEVEL);
                if (fixed > 0) {
                    totalFixed += fixed;
                    player.sendMessage("§eProtectionLimit: §f" + fixed + " item(s) in your inventory were downgraded to Prot III.");
                }
            }
            sender.sendMessage("§aProtectionLimit fix complete. Total items downgraded: §f" + totalFixed);
            return true;
        }
        sender.sendMessage("§cUsage: /protectionlimit fix");
        return true;
    }
}
