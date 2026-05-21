package fr.moodcraft.tradeblocker;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TradeFilter {

    private final MoodVillagerTradeBlocker plugin;
    private final Set<Material> blockedResults = new HashSet<>();
    private final Set<Material> blockedEmeraldIngredients = new HashSet<>();
    private final Map<String, Integer> expensiveBooks = new HashMap<>();
    private boolean blockAllEmeraldResults;

    public TradeFilter(MoodVillagerTradeBlocker plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        blockedResults.clear();
        blockedEmeraldIngredients.clear();
        expensiveBooks.clear();

        FileConfiguration config = plugin.getConfig();
        blockAllEmeraldResults = config.getBoolean("settings.block-all-emerald-results", true);

        for (String value : config.getStringList("blocked-result-items")) addMaterial(blockedResults, value);
        for (String value : config.getStringList("blocked-emerald-ingredient-items")) addMaterial(blockedEmeraldIngredients, value);

        ConfigurationSection expensive = config.getConfigurationSection("expensive-enchanted-books");
        if (expensive != null) {
            for (String key : expensive.getKeys(false)) {
                int amount = Math.max(1, Math.min(64, expensive.getInt(key, 64)));
                expensiveBooks.put(normalize(key), amount);
            }
        }
    }

    public boolean shouldBlock(MerchantRecipe recipe) {
        if (recipe == null) return false;
        ItemStack result = recipe.getResult();
        if (isBlockedResult(result)) return true;
        if (result != null && result.getType() == Material.EMERALD) {
            if (blockAllEmeraldResults) return true;
            for (ItemStack ingredient : recipe.getIngredients()) if (isBlockedEmeraldIngredient(ingredient)) return true;
        }
        return false;
    }

    public MerchantRecipe adjustRecipe(MerchantRecipe recipe) {
        if (recipe == null) return null;
        int minimumEmeralds = minimumEmeraldCost(recipe.getResult());
        if (minimumEmeralds <= 0) return recipe;

        List<ItemStack> ingredients = new ArrayList<>(recipe.getIngredients());
        boolean foundEmerald = false;

        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack ingredient = ingredients.get(i);
            if (ingredient == null || ingredient.getType() != Material.EMERALD) continue;
            foundEmerald = true;
            if (ingredient.getAmount() < minimumEmeralds) {
                ItemStack adjusted = ingredient.clone();
                adjusted.setAmount(minimumEmeralds);
                ingredients.set(i, adjusted);
            }
            break;
        }

        if (!foundEmerald) {
            ingredients.add(new ItemStack(Material.EMERALD, minimumEmeralds));
        }

        recipe.setIngredients(ingredients);
        return recipe;
    }

    private boolean isBlockedResult(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return blockedResults.contains(item.getType());
    }

    private int minimumEmeraldCost(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) return 0;
        if (!(item.getItemMeta() instanceof EnchantmentStorageMeta meta)) return 0;

        int minimum = 0;
        for (Enchantment enchantment : meta.getStoredEnchants().keySet()) {
            String key = normalize(enchantment.getKey().getKey());
            minimum = Math.max(minimum, expensiveBooks.getOrDefault(key, 0));
        }
        return minimum;
    }

    private boolean isBlockedEmeraldIngredient(ItemStack item) {
        return item != null && item.getType() != Material.AIR && blockedEmeraldIngredients.contains(item.getType());
    }

    private void addMaterial(Set<Material> target, String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            target.add(Material.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Matériau inconnu ignoré dans la config: " + raw);
        }
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("minecraft:", "").replace('-', '_').replace(' ', '_');
    }
}
