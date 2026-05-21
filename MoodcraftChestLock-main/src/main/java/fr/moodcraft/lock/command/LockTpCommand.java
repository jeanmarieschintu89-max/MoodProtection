package fr.moodcraft.lock.command;

import fr.moodcraft.lock.MoodLock;
import fr.moodcraft.lock.manager.LockManager;
import fr.moodcraft.lock.util.MoodStyle;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class LockTpCommand implements CommandExecutor {

    private final MoodLock plugin;
    private final LockManager lockManager;

    public LockTpCommand(
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

        if (!player.hasPermission("moodlock.locktp")) {
            MoodStyle.admin(
                    player,
                    "Accès refusé.",
                    MoodStyle.bullet("Permission requise : §emoodlock.locktp")
            );
            return true;
        }

        if (args.length == 0) {
            MoodStyle.admin(
                    player,
                    "Recherche de coffre.",
                    MoodStyle.bullet("Téléporter : §e/locktp <joueur> <id>"),
                    MoodStyle.bullet("Lister : §e/locktp <joueur>")
            );
            return true;
        }

        String owner = args[0];

        if (args.length == 1) {
            sendList(player, owner);
            return true;
        }

        int id;

        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            MoodStyle.admin(
                    player,
                    "ID invalide.",
                    MoodStyle.bullet("Exemple : §e/locktp " + owner + " 1")
            );
            return true;
        }

        Location location =
                lockManager.findChest(owner, id);

        if (location == null) {
            MoodStyle.admin(
                    player,
                    "Coffre introuvable.",
                    MoodStyle.bullet("Joueur : §e" + owner + " §8| §7ID : §e#" + id)
            );
            return true;
        }

        Location target =
                location.clone().add(0.5D, 1.0D, 0.5D);

        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());

        player.teleport(target);

        plugin.getLogger().info(
                "[LOCKTP] "
                        + player.getName()
                        + " s'est téléporté au coffre #"
                        + id
                        + " de "
                        + owner
                        + " en "
                        + location.getWorld().getName()
                        + " "
                        + location.getBlockX()
                        + " "
                        + location.getBlockY()
                        + " "
                        + location.getBlockZ()
        );

        MoodStyle.admin(
                player,
                "Téléportation effectuée.",
                MoodStyle.bullet("Joueur : §e" + owner + " §8| §7Coffre : §e#" + id)
        );

        return true;
    }

    private void sendList(
            Player player,
            String owner
    ) {

        List<LockManager.LockInfo> locks =
                lockManager.listLocks(owner);

        player.sendMessage("");
        player.sendMessage(MoodStyle.header("Coffres Sécurisés"));
        player.sendMessage("");

        if (locks.isEmpty()) {
            player.sendMessage("§c✖ §fAucun coffre trouvé.");
            player.sendMessage(MoodStyle.bullet("Joueur : §e" + owner));
            player.sendMessage("");
            player.sendMessage(MoodStyle.FRAME);
            return;
        }

        player.sendMessage("§e➜ §fCoffres trouvés pour §e" + owner);
        player.sendMessage("");

        for (LockManager.LockInfo lock : locks) {

            Location location = lock.location();

            player.sendMessage(
                    "§8• §e#"
                            + lock.id()
                            + " §8- §f"
                            + location.getWorld().getName()
                            + " §7x:§e"
                            + location.getBlockX()
                            + " §7y:§e"
                            + location.getBlockY()
                            + " §7z:§e"
                            + location.getBlockZ()
            );
        }

        player.sendMessage("");
        player.sendMessage(MoodStyle.FRAME);
    }
}
