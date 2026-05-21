package fr.moodcraft.lock.util;

import fr.moodcraft.lock.MoodLock;
import fr.moodcraft.lock.manager.LockManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class LockUtil {

    private LockUtil() {
    }

    public static boolean isAllowedWorld(
            MoodLock plugin,
            Player player
    ) {

        String allowedWorld =
                plugin.getConfig().getString(
                        "world",
                        "world"
                );

        return player.getWorld().getName().equalsIgnoreCase(allowedWorld);
    }

    public static String allowedWorld(
            MoodLock plugin
    ) {

        return plugin.getConfig().getString(
                "world",
                "world"
        );
    }

    public static boolean isLockableChest(
            Block block
    ) {

        if (block == null) {
            return false;
        }

        Material type = block.getType();

        return type == Material.CHEST
                || type == Material.TRAPPED_CHEST;
    }

    public static Block getTargetChest(
            Player player
    ) {

        Block block =
                player.getTargetBlockExact(5);

        if (!isLockableChest(block)) {
            return null;
        }

        return block;
    }

    public static void removeLinkedSign(
            LockManager lockManager,
            Block chest
    ) {

        String signKey =
                lockManager.getSignKey(chest);

        if (signKey == null || signKey.isEmpty()) {
            return;
        }

        Location signLocation =
                lockManager.fromKey(signKey);

        if (signLocation == null) {
            return;
        }

        Block signBlock =
                signLocation.getBlock();

        if (signBlock == null) {
            return;
        }

        signBlock.setType(Material.AIR);
    }
}
