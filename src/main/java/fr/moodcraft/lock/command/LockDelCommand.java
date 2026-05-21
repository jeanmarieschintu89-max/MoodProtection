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

public class LockDelCommand implements CommandExecutor {

    private final MoodLock plugin;
    private final LockManager lockManager;

    public LockDelCommand(
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
                    "§7Commande: §e/lockdel <joueur>"
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

        boolean removed =
                lockManager.removeMember(chest, member);

        if (!removed) {
            MoodStyle.error(
                    player,
                    "Ce joueur n'était pas membre du coffre.",
                    "§7Joueur: §e" + member
            );
            return true;
        }

        int id =
                lockManager.getLockId(chest);

        plugin.getLogger().info(
                "[LOCKDEL] "
                        + player.getName()
                        + " a retiré "
                        + member
                        + " du coffre #"
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
                "Membre retiré.",
                "§7Joueur: §e" + member,
                "§7Coffre: §e#" + id
        );

        return true;
    }
}
