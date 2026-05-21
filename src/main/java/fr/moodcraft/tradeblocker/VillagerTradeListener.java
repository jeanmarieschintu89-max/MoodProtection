package fr.moodcraft.tradeblocker;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public final class VillagerTradeListener implements Listener {

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final TradeFilter filter;

    public VillagerTradeListener(org.bukkit.plugin.java.JavaPlugin plugin, TradeFilter filter) {
        this.plugin = plugin;
        this.filter = filter;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractMerchant(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Merchant merchant)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> filterMerchant(merchant));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpenMerchantInventory(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!(event.getInventory() instanceof MerchantInventory inventory)) return;
        Merchant merchant = inventory.getMerchant();
        if (merchant == null) return;
        filterMerchant(merchant);
    }

    private void filterMerchant(Merchant merchant) {
        List<MerchantRecipe> original = new ArrayList<>(merchant.getRecipes());
        if (original.isEmpty()) return;

        List<MerchantRecipe> filtered = new ArrayList<>();
        int removed = 0;
        int adjusted = 0;

        for (MerchantRecipe recipe : original) {
            if (filter.shouldBlock(recipe)) {
                removed++;
                continue;
            }
            MerchantRecipe adjustedRecipe = filter.adjustRecipe(recipe);
            if (adjustedRecipe != recipe) adjusted++;
            filtered.add(adjustedRecipe);
        }

        if (removed <= 0 && adjusted <= 0) return;
        merchant.setRecipes(filtered);

        if (plugin.getConfig().getBoolean("settings.notify-admins", false)) {
            plugin.getServer().getLogger().info("[MoodVillagerTradeBlocker] Trades supprimés: " + removed + ", prix ajustés: " + adjusted);
        }
    }
}
