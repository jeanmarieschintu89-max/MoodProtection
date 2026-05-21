package fr.moodcraft.lock.command;

import fr.moodcraft.lock.MoodLock;
import fr.moodcraft.lock.manager.LockContentManager;
import fr.moodcraft.lock.manager.LockManager;
import fr.moodcraft.lock.util.LockUtil;
import fr.moodcraft.lock.util.MoodStyle;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlockCommand implements CommandExecutor {

    private final MoodLock plugin;
    private final LockManager lockManager;
    private final LockContentManager contentManager;

    public UnlockCommand(MoodLock plugin, LockManager lockManager, LockContentManager contentManager) {
        this.plugin = plugin;
        this.lockManager = lockManager;
        this.contentManager = contentManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MoodStyle.error(sender, "Commande joueur uniquement.");
            return true;
        }

        if (!LockUtil.isAllowedWorld(plugin, player)) {
            MoodStyle.error(player, "Commande indisponible ici.", "§7Monde autorisé: §e" + LockUtil.allowedWorld(plugin));
            return true;
        }

        Block chest = LockUtil.getTargetChest(player);
        if (chest == null) {
            MoodStyle.error(player, "Regarde un coffre verrouillé.");
            return true;
        }

        if (!lockManager.isLocked(chest)) {
            MoodStyle.error(player, "Ce coffre n'est pas verrouillé.");
            return true;
        }

        if (!lockManager.isOwner(chest, player.getUniqueId())) {
            MoodStyle.error(player, "Ce coffre ne t'appartient pas.", MoodStyle.bullet("Demande à un administrateur si besoin"));
            return true;
        }

        int id = lockManager.getLockId(chest);
        contentManager.remove(chest);
        LockUtil.removeLinkedSign(lockManager, chest);
        lockManager.unlock(chest);

        plugin.getLogger().info("[UNLOCK] " + player.getName() + " a déverrouillé son coffre #" + id + " en " + chest.getWorld().getName() + " " + chest.getX() + " " + chest.getY() + " " + chest.getZ());

        MoodStyle.success(
                player,
                "Coffre déverrouillé.",
                "§7ID retiré: §e#" + id,
                MoodStyle.bullet("La pancarte a été retirée")
        );

        return true;
    }
}
