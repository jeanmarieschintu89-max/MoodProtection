package fr.moodcraft.lock.listener;

import fr.moodcraft.lock.manager.LockManager;
import fr.moodcraft.lock.util.LockUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LockProtectionListener implements Listener {

    private final LockManager lockManager;

    public LockProtectionListener(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void entityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void blockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pistonPush(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isProtectedBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pistonPull(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isProtectedBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void hopperMove(InventoryMoveItemEvent event) {
        if (isLockedInventory(event.getSource()) || isLockedInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean isProtectedBlock(Block block) {
        if (LockUtil.isLockableChest(block) && lockManager.isLocked(block)) return true;
        return block != null && lockManager.isSignProtected(block.getLocation());
    }

    private boolean isLockedInventory(Inventory inventory) {
        if (inventory == null) return false;
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof Chest chest) {
            return lockManager.isLocked(chest.getBlock());
        }
        if (inventory instanceof DoubleChestInventory doubleChestInventory) {
            return isLockedHolder(doubleChestInventory.getLeftSide().getHolder())
                    || isLockedHolder(doubleChestInventory.getRightSide().getHolder());
        }
        return false;
    }

    private boolean isLockedHolder(InventoryHolder holder) {
        if (holder instanceof Chest chest) return lockManager.isLocked(chest.getBlock());
        return false;
    }
}
