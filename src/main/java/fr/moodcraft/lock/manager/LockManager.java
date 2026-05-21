package fr.moodcraft.lock.manager;

import fr.moodcraft.lock.MoodLock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LockManager {

    private final MoodLock plugin;
    private final File file;

    private YamlConfiguration data;

    private final Map<String, UUID> locks = new HashMap<>();
    private final Map<String, String> names = new HashMap<>();
    private final Map<String, String> signs = new HashMap<>();
    private final Map<String, Integer> ids = new HashMap<>();
    private final Map<String, Set<String>> members = new HashMap<>();

    public LockManager(MoodLock plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locks.yml");
    }

    public void load() {

        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        data = YamlConfiguration.loadConfiguration(file);

        locks.clear();
        names.clear();
        signs.clear();
        ids.clear();
        members.clear();

        ConfigurationSection section = data.getConfigurationSection("locks");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {

            String ownerValue = data.getString("locks." + key + ".owner");

            if (ownerValue == null || ownerValue.isEmpty()) {
                continue;
            }

            try {
                UUID owner = UUID.fromString(ownerValue);
                String name = data.getString("locks." + key + ".name");
                String sign = data.getString("locks." + key + ".sign");
                int id = data.getInt("locks." + key + ".id", 0);

                if (id <= 0) {
                    id = getNextId(owner);
                }

                locks.put(key, owner);
                names.put(key, name);
                ids.put(key, id);

                Set<String> memberSet = new HashSet<>();

                for (String member : data.getStringList("locks." + key + ".members")) {
                    if (member != null && !member.isEmpty()) {
                        memberSet.add(normalize(member));
                    }
                }

                members.put(key, memberSet);

                if (sign != null && !sign.isEmpty()) {
                    signs.put(key, sign);
                }

            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Lock ignoré, UUID invalide : " + key);
            }
        }

        save();
    }

    public void save() {

        if (data == null) {
            data = new YamlConfiguration();
        }

        data.set("locks", null);

        for (String key : locks.keySet()) {
            UUID owner = locks.get(key);

            if (owner == null) {
                continue;
            }

            data.set("locks." + key + ".owner", owner.toString());
            data.set("locks." + key + ".name", names.get(key));
            data.set("locks." + key + ".sign", signs.get(key));
            data.set("locks." + key + ".id", ids.getOrDefault(key, 0));
            data.set("locks." + key + ".members", new ArrayList<>(members.getOrDefault(key, new HashSet<>())));
        }

        try {
            data.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public boolean isLocked(Block block) {
        for (Location location : getChestParts(block)) {
            if (isLocked(location)) {
                return true;
            }
        }

        return false;
    }

    public boolean isLocked(Location location) {
        if (location == null) {
            return false;
        }

        return locks.containsKey(key(location));
    }

    public boolean isOwner(Block block, UUID uuid) {
        Location location = getLockedLocation(block);
        return location != null && isOwner(location, uuid);
    }

    public boolean isOwner(Location location, UUID uuid) {
        if (location == null || uuid == null) {
            return false;
        }

        UUID owner = locks.get(key(location));
        return owner != null && owner.equals(uuid);
    }

    public boolean hasAccess(Block block, UUID uuid, String playerName) {
        Location location = getLockedLocation(block);
        return location != null && hasAccess(location, uuid, playerName);
    }

    public boolean hasAccess(Location location, UUID uuid, String playerName) {
        if (isOwner(location, uuid)) {
            return true;
        }

        Set<String> memberSet = members.get(key(location));

        return memberSet != null
                && playerName != null
                && memberSet.contains(normalize(playerName));
    }

    public int lock(Block block, UUID owner, String ownerName, Location signLocation) {
        int id = getNextId(owner);
        String signKey = signLocation == null ? null : key(signLocation);
        Set<String> memberSet = new HashSet<>();

        for (Location part : getChestParts(block)) {
            String partKey = key(part);
            locks.put(partKey, owner);
            names.put(partKey, ownerName);
            ids.put(partKey, id);
            members.put(partKey, memberSet);

            if (signKey != null) {
                signs.put(partKey, signKey);
            }
        }

        save();
        return id;
    }

    public void unlock(Block block) {
        for (Location part : getChestParts(block)) {
            unlockLocation(part);
        }

        save();
    }

    public void unlock(Location location) {
        unlockLocation(location);
        save();
    }

    private void unlockLocation(Location location) {
        if (location == null) {
            return;
        }

        String key = key(location);
        locks.remove(key);
        names.remove(key);
        signs.remove(key);
        ids.remove(key);
        members.remove(key);
    }

    public boolean addMember(Block block, String playerName) {
        if (!isLocked(block) || playerName == null) {
            return false;
        }

        String normalized = normalize(playerName);

        for (Location part : getChestParts(block)) {
            String key = key(part);

            if (!locks.containsKey(key)) {
                continue;
            }

            members.putIfAbsent(key, new HashSet<>());
            members.get(key).add(normalized);
        }

        save();
        return true;
    }

    public boolean removeMember(Block block, String playerName) {
        if (!isLocked(block) || playerName == null) {
            return false;
        }

        boolean removed = false;
        String normalized = normalize(playerName);

        for (Location part : getChestParts(block)) {
            Set<String> memberSet = members.get(key(part));

            if (memberSet == null) {
                continue;
            }

            if (memberSet.remove(normalized)) {
                removed = true;
            }
        }

        save();
        return removed;
    }

    public boolean isMember(Block block, String playerName) {
        if (playerName == null) {
            return false;
        }

        Location location = getLockedLocation(block);

        if (location == null) {
            return false;
        }

        Set<String> memberSet = members.get(key(location));

        return memberSet != null && memberSet.contains(normalize(playerName));
    }

    public String getOwnerName(Block block) {
        Location location = getLockedLocation(block);
        return location == null ? null : getOwnerName(location);
    }

    public String getOwnerName(Location location) {
        return location == null ? null : names.get(key(location));
    }

    public UUID getOwner(Location location) {
        return location == null ? null : locks.get(key(location));
    }

    public Set<String> getMembers(Location location) {
        if (location == null) {
            return new HashSet<>();
        }

        return new HashSet<>(members.getOrDefault(key(location), new HashSet<>()));
    }

    public int getLockId(Block block) {
        Location location = getLockedLocation(block);
        return location == null ? 0 : getLockId(location);
    }

    public int getLockId(Location location) {
        return location == null ? 0 : ids.getOrDefault(key(location), 0);
    }

    public String getSignKey(Block block) {
        Location location = getLockedLocation(block);
        return location == null ? null : getSignKey(location);
    }

    public String getSignKey(Location location) {
        return location == null ? null : signs.get(key(location));
    }

    public boolean isSignProtected(Location location) {
        if (location == null) {
            return false;
        }

        return signs.containsValue(key(location));
    }

    public int countLocks(UUID owner) {
        Set<Integer> uniqueIds = new HashSet<>();

        for (String key : locks.keySet()) {
            UUID currentOwner = locks.get(key);

            if (currentOwner == null || !currentOwner.equals(owner)) {
                continue;
            }

            uniqueIds.add(ids.getOrDefault(key, 0));
        }

        return uniqueIds.size();
    }

    public int totalLocks() {
        Set<String> unique = new HashSet<>();

        for (String key : locks.keySet()) {
            UUID owner = locks.get(key);
            int id = ids.getOrDefault(key, 0);
            unique.add((owner == null ? "unknown" : owner.toString()) + ";" + id);
        }

        return unique.size();
    }

    public int transferOwner(String oldOwnerName, UUID newOwner, String newOwnerName) {
        if (oldOwnerName == null || newOwner == null || newOwnerName == null) {
            return 0;
        }

        Set<Integer> changed = new HashSet<>();

        for (String key : new ArrayList<>(names.keySet())) {
            String ownerName = names.get(key);

            if (ownerName == null || !ownerName.equalsIgnoreCase(oldOwnerName)) {
                continue;
            }

            locks.put(key, newOwner);
            names.put(key, newOwnerName);
            changed.add(ids.getOrDefault(key, 0));
        }

        if (!changed.isEmpty()) {
            save();
        }

        return changed.size();
    }

    public int purgeOwner(String ownerName) {
        if (ownerName == null) {
            return 0;
        }

        Set<Integer> removed = new HashSet<>();

        for (String key : new ArrayList<>(names.keySet())) {
            String currentName = names.get(key);

            if (currentName == null || !currentName.equalsIgnoreCase(ownerName)) {
                continue;
            }

            removed.add(ids.getOrDefault(key, 0));
            locks.remove(key);
            names.remove(key);
            signs.remove(key);
            ids.remove(key);
            members.remove(key);
        }

        if (!removed.isEmpty()) {
            save();
        }

        return removed.size();
    }

    public int cleanupBrokenLocks() {
        Set<Integer> removed = new HashSet<>();

        for (String key : new ArrayList<>(locks.keySet())) {
            Location location = fromKey(key);

            if (location != null && isLockableChest(location.getBlock())) {
                continue;
            }

            removed.add(ids.getOrDefault(key, 0));
            locks.remove(key);
            names.remove(key);
            signs.remove(key);
            ids.remove(key);
            members.remove(key);
        }

        if (!removed.isEmpty()) {
            save();
        }

        return removed.size();
    }

    public Location findChest(String playerName, int id) {
        for (String key : names.keySet()) {
            String ownerName = names.get(key);

            if (ownerName == null || !ownerName.equalsIgnoreCase(playerName)) {
                continue;
            }

            int currentId = ids.getOrDefault(key, 0);

            if (currentId != id) {
                continue;
            }

            return fromKey(key);
        }

        return null;
    }

    public List<LockInfo> listLocks(String playerName) {
        List<LockInfo> list = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        for (String key : names.keySet()) {
            String ownerName = names.get(key);

            if (ownerName == null || !ownerName.equalsIgnoreCase(playerName)) {
                continue;
            }

            int id = ids.getOrDefault(key, 0);

            if (seenIds.contains(id)) {
                continue;
            }

            Location location = fromKey(key);

            if (location == null) {
                continue;
            }

            seenIds.add(id);
            list.add(new LockInfo(id, ownerName, location));
        }

        list.sort(Comparator.comparingInt(LockInfo::id));
        return list;
    }

    public List<Location> getLockedLocations() {
        List<Location> locations = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String key : locks.keySet()) {
            Location location = fromKey(key);
            if (location == null || !isLockableChest(location.getBlock())) continue;
            int id = ids.getOrDefault(key, 0);
            UUID owner = locks.get(key);
            String unique = (owner == null ? "unknown" : owner.toString()) + ";" + id;
            if (!seen.add(unique)) continue;
            locations.add(location);
        }

        return locations;
    }

    private Location getLockedLocation(Block block) {
        for (Location location : getChestParts(block)) {
            if (isLocked(location)) {
                return location;
            }
        }

        return null;
    }

    public List<Location> getChestParts(Block block) {
        List<Location> parts = new ArrayList<>();

        if (!isLockableChest(block)) {
            return parts;
        }

        Chest chest = (Chest) block.getState();
        Inventory inventory = chest.getInventory();

        if (inventory instanceof DoubleChestInventory doubleChestInventory) {
            addChestHolder(parts, doubleChestInventory.getLeftSide().getHolder());
            addChestHolder(parts, doubleChestInventory.getRightSide().getHolder());
            return parts;
        }

        parts.add(block.getLocation());
        return parts;
    }

    private void addChestHolder(List<Location> parts, InventoryHolder holder) {
        if (holder instanceof Chest chest) {
            parts.add(chest.getLocation());
        }
    }

    private boolean isLockableChest(Block block) {
        if (block == null) {
            return false;
        }

        Material type = block.getType();

        return type == Material.CHEST || type == Material.TRAPPED_CHEST;
    }

    private int getNextId(UUID owner) {
        int highest = 0;

        for (String key : locks.keySet()) {
            UUID currentOwner = locks.get(key);

            if (currentOwner == null || !currentOwner.equals(owner)) {
                continue;
            }

            int id = ids.getOrDefault(key, 0);

            if (id > highest) {
                highest = id;
            }
        }

        return highest + 1;
    }

    public String key(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown;0;0;0";
        }

        return location.getWorld().getName()
                + ";"
                + location.getBlockX()
                + ";"
                + location.getBlockY()
                + ";"
                + location.getBlockZ();
    }

    public Location fromKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        try {
            String[] split = key.split(";");

            if (split.length != 4) {
                return null;
            }

            World world = Bukkit.getWorld(split[0]);

            if (world == null) {
                return null;
            }

            return new Location(
                    world,
                    Integer.parseInt(split[1]),
                    Integer.parseInt(split[2]),
                    Integer.parseInt(split[3])
            );

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    public record LockInfo(int id, String owner, Location location) {
    }
}
