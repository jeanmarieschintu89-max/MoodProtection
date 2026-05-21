package fr.moodcraft.lock.manager;

import fr.moodcraft.lock.MoodLock;
import fr.moodcraft.lock.util.LockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LockContentManager {

    private static final long SAVE_DELAY_TICKS = 20L * 10L;

    private final MoodLock plugin;
    private final LockManager lockManager;
    private final File activeFile;
    private final File historyFile;
    private YamlConfiguration activeData;
    private YamlConfiguration historyData;
    private boolean saveQueued;

    public LockContentManager(MoodLock plugin, LockManager lockManager) {
        this.plugin = plugin;
        this.lockManager = lockManager;
        this.activeFile = new File(plugin.getDataFolder(), "contents.yml");
        this.historyFile = new File(plugin.getDataFolder(), "contents-history.yml");
    }

    public void load() {
        ensureFile(activeFile);
        ensureFile(historyFile);
        activeData = YamlConfiguration.loadConfiguration(activeFile);
        historyData = YamlConfiguration.loadConfiguration(historyFile);
    }

    public void save() {
        if (activeData == null) activeData = new YamlConfiguration();
        if (historyData == null) historyData = new YamlConfiguration();
        try {
            activeData.save(activeFile);
            historyData.save(historyFile);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void snapshot(Block block, String reason) {
        if (!LockUtil.isLockableChest(block) || !lockManager.isLocked(block)) return;
        Inventory inventory = inventoryOf(block);
        if (inventory == null) return;

        List<ItemStack> snapshot = cloneContents(inventory.getContents());
        int count = countItems(snapshot);
        if (count <= 0) return;

        long now = System.currentTimeMillis();
        boolean startup = "startup".equalsIgnoreCase(reason);
        int parts = 0;

        for (Location part : lockManager.getChestParts(block)) {
            String key = lockManager.key(part);
            activeData.set("contents." + key + ".items", snapshot);
            activeData.set("contents." + key + ".size", inventory.getSize());
            activeData.set("contents." + key + ".updated", now);
            activeData.set("contents." + key + ".lock-id", lockManager.getLockId(block));
            activeData.set("contents." + key + ".owner", lockManager.getOwnerName(block));

            if (!startup) {
                archive(key, snapshot, inventory.getSize(), lockManager.getLockId(block), lockManager.getOwnerName(block), reason, now, count);
            }
            parts++;
        }

        requestSave();
        if (!startup) {
            plugin.getLogger().info("[LOCK-CONTENT] Snapshot " + reason + " coffre #" + lockManager.getLockId(block) + " parts=" + parts + " items=" + count + " at " + describe(block));
        }
    }

    public boolean restoreIfEmpty(Block block, String reason) {
        if (!LockUtil.isLockableChest(block) || !lockManager.isLocked(block)) return false;
        Inventory inventory = inventoryOf(block);
        if (inventory == null || !isEmpty(inventory)) return false;

        Location locked = findStoredPart(block);
        if (locked == null) return false;

        List<ItemStack> saved = readActiveItems(lockManager.key(locked));
        int count = countItems(saved);
        if (count <= 0) return false;

        inventory.setContents(toArray(saved, inventory.getSize()));
        archive(lockManager.key(locked), saved, inventory.getSize(), lockManager.getLockId(block), lockManager.getOwnerName(block), "restore-" + reason, System.currentTimeMillis(), count);
        requestSave();
        plugin.getLogger().warning("[LOCK-CONTENT] Restauration " + reason + " coffre #" + lockManager.getLockId(block) + " items=" + count + " at " + describe(block));
        return true;
    }

    public void remove(Block block) {
        if (!LockUtil.isLockableChest(block)) return;
        long now = System.currentTimeMillis();
        int archived = 0;

        for (Location part : lockManager.getChestParts(block)) {
            String key = lockManager.key(part);
            List<ItemStack> saved = readActiveItems(key);
            int count = countItems(saved);
            if (count > 0) {
                archive(key, saved, activeData.getInt("contents." + key + ".size", saved.size()), lockManager.getLockId(block), lockManager.getOwnerName(block), "unlock", now, count);
                archived += count;
            }
            activeData.set("contents." + key, null);
        }

        requestSave();
        plugin.getLogger().info("[LOCK-CONTENT] Backup actif supprimé coffre #" + lockManager.getLockId(block) + " archive-items=" + archived + " at " + describe(block));
    }

    public ContentSummary activeSummary(Location location) {
        if (location == null) return ContentSummary.empty("inconnu", 0, "Aucun emplacement.");
        String key = lockManager.key(location);
        List<ItemStack> items = readActiveItems(key);
        return new ContentSummary(key, activeData.getLong("contents." + key + ".updated", 0L), activeData.getString("contents." + key + ".owner", "inconnu"), activeData.getInt("contents." + key + ".lock-id", 0), countItems(items), summarize(items));
    }

    public List<ContentSummary> historySummaries(String ownerName, int lockId, int limit) {
        List<ContentSummary> result = new ArrayList<>();
        ConfigurationSection history = historyData.getConfigurationSection("history");
        if (history == null) return result;

        for (String key : history.getKeys(false)) {
            String path = "history." + key;
            String owner = historyData.getString(path + ".owner", "");
            int id = historyData.getInt(path + ".lock-id", 0);
            if (lockId > 0 && id != lockId) continue;
            if (ownerName != null && !ownerName.isBlank() && !owner.equalsIgnoreCase(ownerName)) continue;
            List<ItemStack> items = readHistoryItems(key);
            result.add(new ContentSummary(historyData.getString(path + ".source", "inconnu"), historyData.getLong(path + ".updated", 0L), owner, id, historyData.getInt(path + ".count", countItems(items)), summarize(items), historyData.getString(path + ".reason", "snapshot")));
        }

        result.sort(Comparator.comparingLong(ContentSummary::updated).reversed());
        if (result.size() > limit) return new ArrayList<>(result.subList(0, limit));
        return result;
    }

    private void requestSave() {
        if (saveQueued) return;
        saveQueued = true;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            saveQueued = false;
            save();
        }, SAVE_DELAY_TICKS);
    }

    private List<ItemStack> readHistoryItems(String historyKey) {
        List<?> raw = historyData.getList("history." + historyKey + ".items");
        List<ItemStack> result = new ArrayList<>();
        if (raw == null) return result;
        for (Object object : raw) result.add(object instanceof ItemStack item ? item.clone() : null);
        return result;
    }

    private List<String> summarize(List<ItemStack> items) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) continue;
            String name = formatMaterial(item.getType());
            map.put(name, map.getOrDefault(name, 0) + item.getAmount());
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) lines.add("§8• §e" + entry.getValue() + "x §f" + entry.getKey());
        return lines;
    }

    private void archive(String sourceKey, List<ItemStack> items, int size, int lockId, String owner, String reason, long now, int count) {
        if (count <= 0) return;
        String archiveKey = now + "_" + sanitize(sourceKey) + "_" + sanitize(reason);
        String path = "history." + archiveKey;
        historyData.set(path + ".source", sourceKey);
        historyData.set(path + ".reason", reason);
        historyData.set(path + ".updated", now);
        historyData.set(path + ".lock-id", lockId);
        historyData.set(path + ".owner", owner);
        historyData.set(path + ".size", size);
        historyData.set(path + ".count", count);
        historyData.set(path + ".items", cloneList(items));
    }

    private void ensureFile(File file) {
        if (file.exists()) return;
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            file.createNewFile();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private Location findStoredPart(Block block) {
        for (Location part : lockManager.getChestParts(block)) {
            String key = lockManager.key(part);
            if (activeData.isList("contents." + key + ".items")) return part;
        }
        return null;
    }

    private Inventory inventoryOf(Block block) {
        if (!LockUtil.isLockableChest(block)) return null;
        if (!(block.getState() instanceof Chest chest)) return null;
        return chest.getInventory();
    }

    private List<ItemStack> readActiveItems(String key) {
        List<?> raw = activeData.getList("contents." + key + ".items");
        List<ItemStack> result = new ArrayList<>();
        if (raw == null) return result;
        for (Object object : raw) result.add(object instanceof ItemStack item ? item.clone() : null);
        return result;
    }

    private List<ItemStack> cloneContents(ItemStack[] contents) {
        List<ItemStack> result = new ArrayList<>();
        if (contents == null) return result;
        for (ItemStack item : contents) result.add(item == null ? null : item.clone());
        return result;
    }

    private List<ItemStack> cloneList(List<ItemStack> items) {
        List<ItemStack> result = new ArrayList<>();
        if (items == null) return result;
        for (ItemStack item : items) result.add(item == null ? null : item.clone());
        return result;
    }

    private ItemStack[] toArray(List<ItemStack> list, int size) {
        ItemStack[] result = new ItemStack[size];
        for (int i = 0; i < size && i < list.size(); i++) {
            ItemStack item = list.get(i);
            result[i] = item == null ? null : item.clone();
        }
        return result;
    }

    private boolean isEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) return false;
        }
        return true;
    }

    private int countItems(List<ItemStack> items) {
        int count = 0;
        if (items == null) return 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) count += item.getAmount();
        }
        return count;
    }

    private String formatMaterial(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.replace(';', '_').replace(':', '_').replace('.', '_').replace(' ', '_').replace('/', '_').replace('\\', '_');
    }

    private String describe(Block block) {
        if (block == null || block.getWorld() == null) return "unknown";
        return block.getWorld().getName() + " " + block.getX() + " " + block.getY() + " " + block.getZ();
    }

    public record ContentSummary(String source, long updated, String owner, int lockId, int count, List<String> lines, String reason) {
        public ContentSummary(String source, long updated, String owner, int lockId, int count, List<String> lines) {
            this(source, updated, owner, lockId, count, lines, "active");
        }

        public static ContentSummary empty(String source, int lockId, String message) {
            return new ContentSummary(source, 0L, "inconnu", lockId, 0, List.of("§8• §7" + message), "empty");
        }

        public String formattedDate() {
            if (updated <= 0) return "inconnue";
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(updated));
        }
    }
}
