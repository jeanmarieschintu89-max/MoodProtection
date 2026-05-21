package fr.moodcraft.tradeblocker;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class MoodVillagerTradeBlocker extends JavaPlugin implements CommandExecutor {

    private TradeFilter tradeFilter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        tradeFilter = new TradeFilter(this);
        getServer().getPluginManager().registerEvents(new VillagerTradeListener(this, tradeFilter), this);
        if (getCommand("tradeblockerreload") != null) getCommand("tradeblockerreload").setExecutor(this);
        getLogger().info("MoodVillagerTradeBlocker actif.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MoodVillagerTradeBlocker désactivé.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        reloadConfig();
        tradeFilter.reload();
        sender.sendMessage("§a✔ §fMoodVillagerTradeBlocker rechargé.");
        return true;
    }
}
