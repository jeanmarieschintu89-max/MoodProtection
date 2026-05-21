package fr.moodcraft.lock.command;

import fr.moodcraft.lock.manager.LockContentManager;
import fr.moodcraft.lock.manager.LockManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class LockAdminCommand implements CommandExecutor {

    private final LockManager lockManager;
    private final LockContentManager contentManager;

    public LockAdminCommand(LockManager lockManager, LockContentManager contentManager) {
        this.lockManager = lockManager;
        this.contentManager = contentManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("moodlock.admin")) {
            error(sender, "Accès réservé à l'administration des coffres.");
            return true;
        }

        if (args.length == 0) {
            help(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "help", "aide" -> help(sender);
            case "status", "etat", "état" -> status(sender);
            case "info" -> info(sender);
            case "list", "liste" -> list(sender, args);
            case "content", "contenu" -> content(sender, args);
            case "history", "historique" -> history(sender, args);
            case "tp" -> tp(sender, args);
            case "transfer" -> transfer(sender, args);
            case "purge" -> purge(sender, args);
            case "cleanup", "clean" -> cleanup(sender);
            case "save" -> save(sender);
            case "reload" -> reload(sender);
            default -> help(sender);
        }

        return true;
    }

    private void status(CommandSender sender) {
        header(sender, "Admin Coffres");
        sender.sendMessage("§e➜ §7Coffres verrouillés : §e" + lockManager.totalLocks());
        footer(sender);
    }

    private void info(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            error(sender, "Commande joueur uniquement pour viser un coffre.");
            return;
        }

        Block block = player.getTargetBlockExact(6);
        if (block == null || !lockManager.isLocked(block)) {
            error(sender, "Aucun coffre verrouillé visé.");
            return;
        }

        Location location = block.getLocation();
        String owner = lockManager.getOwnerName(location);
        UUID ownerUuid = lockManager.getOwner(location);
        Set<String> members = lockManager.getMembers(location);

        header(sender, "Info Coffre");
        sender.sendMessage("§e➜ §7ID : §e" + lockManager.getLockId(location));
        sender.sendMessage("§e➜ §7Propriétaire : §e" + (owner == null ? "Inconnu" : owner));
        sender.sendMessage("§e➜ §7UUID : §f" + (ownerUuid == null ? "Inconnu" : ownerUuid));
        sender.sendMessage("§e➜ §7Position : §e" + format(location));
        sender.sendMessage("§e➜ §7Membres : §e" + (members.isEmpty() ? "aucun" : String.join(", ", members)));
        footer(sender);
    }

    private void list(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/lockadmin list <joueur>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String name = safeName(target);
        List<LockManager.LockInfo> locks = lockManager.listLocks(name);

        header(sender, "Liste Coffres");
        sender.sendMessage("§e➜ §7Joueur : §e" + name);
        sender.sendMessage("§e➜ §7Total : §e" + locks.size());

        int shown = 0;
        for (LockManager.LockInfo info : locks) {
            sender.sendMessage("§e➜ §7#" + info.id() + " §8• §e" + format(info.location()));
            shown++;
            if (shown >= 10) break;
        }

        if (locks.size() > shown) sender.sendMessage("§e➜ §7Et §e" + (locks.size() - shown) + " §7autres coffres.");
        footer(sender);
    }

    private void content(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "/lockadmin content <joueur> <id>");
            return;
        }

        Integer id = parseInt(args[2]);
        if (id == null) {
            error(sender, "ID invalide.");
            return;
        }

        Location location = lockManager.findChest(args[1], id);
        if (location == null) {
            error(sender, "Coffre introuvable.");
            return;
        }

        LockContentManager.ContentSummary summary = contentManager.activeSummary(location);
        header(sender, "Contenu Coffre");
        sender.sendMessage("§e➜ §7Joueur : §e" + args[1]);
        sender.sendMessage("§e➜ §7ID : §e#" + id);
        sender.sendMessage("§e➜ §7Position : §e" + format(location));
        sender.sendMessage("§e➜ §7Dernier snapshot : §e" + summary.formattedDate());
        sender.sendMessage("§e➜ §7Items total : §e" + summary.count());
        sender.sendMessage("");
        for (String line : limit(summary.lines(), 20)) sender.sendMessage(line);
        if (summary.lines().size() > 20) sender.sendMessage("§8• §7+ " + (summary.lines().size() - 20) + " lignes masquées.");
        footer(sender);
    }

    private void history(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "/lockadmin history <joueur> <id>");
            return;
        }

        Integer id = parseInt(args[2]);
        if (id == null) {
            error(sender, "ID invalide.");
            return;
        }

        List<LockContentManager.ContentSummary> history = contentManager.historySummaries(args[1], id, 5);
        header(sender, "Historique Coffre");
        sender.sendMessage("§e➜ §7Joueur : §e" + args[1]);
        sender.sendMessage("§e➜ §7ID : §e#" + id);
        sender.sendMessage("§e➜ §7Snapshots affichés : §e" + history.size());

        if (history.isEmpty()) {
            sender.sendMessage("§8• §7Aucun snapshot trouvé.");
            footer(sender);
            return;
        }

        for (LockContentManager.ContentSummary summary : history) {
            sender.sendMessage("");
            sender.sendMessage("§6#" + summary.lockId() + " §8• §7" + summary.reason() + " §8• §e" + summary.formattedDate() + " §8• §7items: §e" + summary.count());
            for (String line : limit(summary.lines(), 8)) sender.sendMessage(line);
            if (summary.lines().size() > 8) sender.sendMessage("§8• §7+ " + (summary.lines().size() - 8) + " lignes masquées.");
        }

        footer(sender);
    }

    private void tp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            error(sender, "Commande joueur uniquement.");
            return;
        }

        if (args.length < 3) {
            usage(sender, "/lockadmin tp <joueur> <id>");
            return;
        }

        Integer id = parseInt(args[2]);
        if (id == null) {
            error(sender, "ID invalide.");
            return;
        }

        Location location = lockManager.findChest(args[1], id);
        if (location == null) {
            error(sender, "Coffre introuvable.");
            return;
        }

        player.teleport(location.clone().add(0.5, 1, 0.5));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
        success(sender, "Téléporté au coffre §e#" + id + " §7de §e" + args[1]);
    }

    private void transfer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "/lockadmin transfer <ancien> <nouveau>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        int count = lockManager.transferOwner(args[1], target.getUniqueId(), safeName(target));
        success(sender, "Coffres transférés : §e" + count + " §7vers §e" + safeName(target));
    }

    private void purge(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/lockadmin purge <joueur>");
            return;
        }

        int count = lockManager.purgeOwner(args[1]);
        success(sender, "Coffres supprimés pour §e" + args[1] + " §8: §e" + count);
    }

    private void cleanup(CommandSender sender) {
        int count = lockManager.cleanupBrokenLocks();
        success(sender, "Locks orphelins nettoyés : §e" + count);
    }

    private void save(CommandSender sender) {
        lockManager.save();
        contentManager.save();
        success(sender, "Données coffres sauvegardées.");
    }

    private void reload(CommandSender sender) {
        lockManager.load();
        contentManager.load();
        success(sender, "Données coffres rechargées.");
    }

    private List<String> limit(List<String> lines, int max) {
        if (lines == null) return List.of();
        if (lines.size() <= max) return lines;
        return lines.subList(0, max);
    }

    private Integer parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeName(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : "Inconnu";
    }

    private String format(Location location) {
        if (location == null || location.getWorld() == null) return "Inconnue";
        return location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

    private void help(CommandSender sender) {
        header(sender, "Admin Coffres");
        sender.sendMessage("§e➜ §7/lockadmin status");
        sender.sendMessage("§e➜ §7/lockadmin info");
        sender.sendMessage("§e➜ §7/lockadmin list <joueur>");
        sender.sendMessage("§e➜ §7/lockadmin content <joueur> <id>");
        sender.sendMessage("§e➜ §7/lockadmin history <joueur> <id>");
        sender.sendMessage("§e➜ §7/lockadmin tp <joueur> <id>");
        sender.sendMessage("§e➜ §7/lockadmin transfer <ancien> <nouveau>");
        sender.sendMessage("§e➜ §7/lockadmin purge <joueur>");
        sender.sendMessage("§e➜ §7/lockadmin cleanup");
        sender.sendMessage("§e➜ §7/lockadmin save");
        sender.sendMessage("§e➜ §7/lockadmin reload");
        footer(sender);
    }

    private void usage(CommandSender sender, String usage) {
        header(sender, "Admin Coffres");
        sender.sendMessage("§c✖ §fCommande incomplète.");
        sender.sendMessage("§e➜ §7Utilisation : §e" + usage);
        footer(sender);
    }

    private void success(CommandSender sender, String message) {
        header(sender, "Admin Coffres");
        sender.sendMessage("§a✔ §f" + message);
        footer(sender);
    }

    private void error(CommandSender sender, String message) {
        header(sender, "Admin Coffres");
        sender.sendMessage("§c✖ §f" + message);
        footer(sender);
    }

    private void header(CommandSender sender, String title) {
        sender.sendMessage("");
        sender.sendMessage("§8----- §6✦ " + title + " ✦ §8-----");
    }

    private void footer(CommandSender sender) {
        sender.sendMessage("§8-----------------------------");
    }
}
