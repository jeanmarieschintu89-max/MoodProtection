package fr.moodcraft.lock.command;

import fr.moodcraft.lock.MoodLock;
import fr.moodcraft.lock.manager.LockManager;
import fr.moodcraft.lock.util.LockUtil;
import fr.moodcraft.lock.util.MoodStyle;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminUnlockCommand implements CommandExecutor {

    private final MoodLock plugin;
    private final LockManager lockManager;

    public AdminUnlockCommand(
            MoodLock plugin,
            LockManager lockManager
    ) {

        this.plugin = plugin;
        this.lockManager = lockManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            MoodStyle.error(sender, "Commande joueur uniquement.");
            return true;
        }

        if (!player.hasPermission("moodlock.adminunlock")) {
            MoodStyle.admin(
                    player,
                    "§c✖ §fAccès refusé.",
                    MoodStyle.bullet("Permission requise: §emoodlock.adminunlock")
            );
            return true;
        }

        Block chest =
                LockUtil.getTargetChest(player);

        if (chest == null) {
            MoodStyle.admin(
                    player,
                    "§c✖ §fRegarde un coffre verrouillé."
            );
            return true;
        }

        if (!lockManager.isLocked(chest)) {
            MoodStyle.admin(
                    player,
                    "§c✖ §fCe coffre n'est pas verrouillé."
            );
            return true;
        }

        String oldOwner =
                lockManager.getOwnerName(chest);

        int id =
                lockManager.getLockId(chest);

        LockUtil.removeLinkedSign(
                lockManager,
                chest
        );

        lockManager.unlock(chest);

        plugin.getLogger().warning(
                "[ADMINUNLOCK] "
                        + player.getName()
                        + " a forcé le déverrouillage du coffre #"
                        + id
                        + " de "
                        + oldOwner
                        + " en "
                        + chest.getWorld().getName()
                        + " "
                        + chest.getX()
                        + " "
                        + chest.getY()
                        + " "
                        + chest.getZ()
        );

        MoodStyle.admin(
                player,
                "§a✔ §fCoffre déverrouillé de force.",
                "§7Ancien propriétaire: §e" + oldOwner,
                "§7ID retiré: §e#" + id,
                MoodStyle.bullet("Action inscrite dans les logs")
        );

        return true;
    }
}
