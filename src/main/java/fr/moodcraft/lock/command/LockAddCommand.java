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

public class LockAddCommand implements CommandExecutor {

    private final MoodLock plugin;
    private final LockManager lockManager;

    public LockAddCommand(
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

        if (args.length == 0) {
            MoodStyle.error(
                    player,
                    "Utilisation incorrecte.",
                    "§7Commande: §e/lockadd <joueur>"
            );
            return true;
        }

        Block chest =
                LockUtil.getTargetChest(player);

        if (chest == null) {
            MoodStyle.error(
                    player,
                    "Regarde un coffre verrouillé."
            );
            return true;
        }

        if (!lockManager.isLocked(chest)) {
            MoodStyle.error(
                    player,
                    "Ce coffre n'est pas verrouillé."
            );
            return true;
        }

        if (!lockManager.isOwner(chest, player.getUniqueId())
                && !player.hasPermission("moodlock.admin")) {

            MoodStyle.error(
                    player,
                    "Tu n'es pas propriétaire de ce coffre."
            );
            return true;
        }

        String member = args[0];

        if (member.equalsIgnoreCase(player.getName())) {
            MoodStyle.error(
                    player,
                    "Tu es déjà propriétaire du coffre."
            );
            return true;
        }

        if (lockManager.isMember(chest, member)) {
            MoodStyle.error(
                    player,
                    "Ce joueur est déjà membre du coffre.",
                    "§7Joueur: §e" + member
            );
            return true;
        }

        lockManager.addMember(chest, member);

        int id =
                lockManager.getLockId(chest);

        plugin.getLogger().info(
                "[LOCKADD] "
                        + player.getName()
                        + " a ajouté "
                        + member
                        + " au coffre #"
                        + id
                        + " en "
                        + chest.getWorld().getName()
                        + " "
                        + chest.getX()
                        + " "
                        + chest.getY()
                        + " "
                        + chest.getZ()
        );

        MoodStyle.success(
                player,
                "Membre ajouté.",
                "§7Joueur: §e" + member,
                "§7Coffre: §e#" + id
        );

        return true;
    }
}
