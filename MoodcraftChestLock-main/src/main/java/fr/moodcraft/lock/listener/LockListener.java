package fr.moodcraft.lock.listener;

import fr.moodcraft.lock.manager.LockContentManager;
import fr.moodcraft.lock.manager.LockManager;
import fr.moodcraft.lock.util.LockUtil;
import fr.moodcraft.lock.util.MoodStyle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

public class LockListener implements Listener {

    private final LockManager lockManager;
    private final LockContentManager contentManager;

    public LockListener(LockManager lockManager, LockContentManager contentManager) {
        this.lockManager = lockManager;
        this.contentManager = contentManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void interact(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();

        if (!LockUtil.isLockableChest(block)) {
            return;
        }

        if (!lockManager.isLocked(block)) {
            return;
        }

        Player player = event.getPlayer();

        if (lockManager.hasAccess(block, player.getUniqueId(), player.getName()) || player.hasPermission("moodlock.admin")) {
            contentManager.restoreIfEmpty(block, "open");
            return;
        }

        event.setCancelled(true);

        String owner = lockManager.getOwnerName(block);

        MoodStyle.error(
                player,
                "Accès refusé.",
                "§7Propriétaire: §e" + owner,
                MoodStyle.bullet("Demande une invitation avec §e/lockadd")
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void inventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof org.bukkit.block.Chest chest)) return;
        Block block = chest.getBlock();
        if (!LockUtil.isLockableChest(block) || !lockManager.isLocked(block)) return;
        contentManager.snapshot(block, "close");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (LockUtil.isLockableChest(block) && lockManager.isLocked(block)) {
            contentManager.snapshot(block, "break-blocked");
            event.setCancelled(true);

            MoodStyle.error(
                    player,
                    "Ce coffre est sécurisé.",
                    MoodStyle.bullet("Utilise §e/unlock §7avant de le casser"),
                    MoodStyle.bullet("Admin: §e/adminunlock")
            );
            return;
        }

        if (lockManager.isSignProtected(block.getLocation())) {
            event.setCancelled(true);

            MoodStyle.error(
                    player,
                    "Cette pancarte protège un coffre sécurisé.",
                    MoodStyle.bullet("Déverrouille le coffre pour la retirer")
            );
        }
    }
}
