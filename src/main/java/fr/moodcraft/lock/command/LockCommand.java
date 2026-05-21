package fr.moodcraft.lock.command;

import fr.moodcraft.lock.MoodLock;
import fr.moodcraft.lock.manager.LockContentManager;
import fr.moodcraft.lock.manager.LockManager;
import fr.moodcraft.lock.util.LockUtil;
import fr.moodcraft.lock.util.MoodStyle;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class LockCommand implements CommandExecutor {

    private final MoodLock plugin;
    private final LockManager lockManager;
    private final LockContentManager contentManager;

    public LockCommand(MoodLock plugin, LockManager lockManager, LockContentManager contentManager) {
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
            MoodStyle.error(player, "Regarde un coffre.", MoodStyle.bullet("Vise le coffre à protéger"));
            return true;
        }

        if (lockManager.isLocked(chest)) {
            MoodStyle.error(player, "Ce coffre est déjà verrouillé.");
            return true;
        }

        int limit = getLimit(player);
        int currentLocks = lockManager.countLocks(player.getUniqueId());
        if (currentLocks >= limit) {
            MoodStyle.error(player, "Limite de coffres atteinte.", "§7Coffres: §e" + currentLocks + "§7/§e" + limit, MoodStyle.bullet("Déverrouille un ancien coffre"));
            return true;
        }

        SignPlace signPlace = findSignPlace(chest, player);
        if (signPlace == null) {
            MoodStyle.error(player, "Aucun emplacement libre devant toi.", MoodStyle.bullet("Vise une face libre du coffre"), MoodStyle.bullet("La pancarte se pose devant"));
            return true;
        }

        if (!placeSignBase(signPlace)) {
            MoodStyle.error(player, "Impossible de poser la pancarte.", MoodStyle.bullet("Vérifie l'espace devant le coffre"));
            return true;
        }

        int id = lockManager.lock(chest, player.getUniqueId(), player.getName(), signPlace.block().getLocation());
        updateSign(signPlace.block(), player.getName(), id);
        contentManager.snapshot(chest, "lock");

        plugin.getLogger().info("[LOCK] " + player.getName() + " a verrouillé le coffre #" + id + " en " + chest.getWorld().getName() + " " + chest.getX() + " " + chest.getY() + " " + chest.getZ());

        MoodStyle.success(
                player,
                "Coffre verrouillé.",
                "§7Propriétaire: §e" + player.getName(),
                "§7ID du coffre: §e#" + id,
                MoodStyle.bullet("Ajouter: §e/lockadd <joueur>"),
                MoodStyle.bullet("Retirer: §e/lockdel <joueur>"),
                MoodStyle.bullet("Déverrouiller: §e/unlock")
        );
        return true;
    }

    private int getLimit(Player player) {
        if (player.hasPermission("moodlock.vip")) return plugin.getConfig().getInt("limits.vip", 15);
        return plugin.getConfig().getInt("limits.member", 5);
    }

    private boolean placeSignBase(SignPlace signPlace) {
        Material material = getSignMaterial();
        if (material == null || !material.name().endsWith("_WALL_SIGN")) material = Material.BIRCH_WALL_SIGN;
        signPlace.block().setType(material, false);

        BlockData data = signPlace.block().getBlockData();
        if (!(data instanceof WallSign wallSign)) return false;
        wallSign.setFacing(signPlace.wallFacing());
        signPlace.block().setBlockData(wallSign, false);
        return true;
    }

    private Material getSignMaterial() {
        String name = plugin.getConfig().getString("sign.material", "BIRCH_WALL_SIGN");
        if (name == null) return Material.BIRCH_WALL_SIGN;
        Material material = Material.matchMaterial(name);
        return material == null ? Material.BIRCH_WALL_SIGN : material;
    }

    private void updateSign(Block signBlock, String playerName, int id) {
        if (!(signBlock.getState() instanceof Sign sign)) return;
        sign.setLine(0, formatSignLine("sign.line1", "§c✦ SÉCURISÉ ✦", playerName, id));
        sign.setLine(1, formatSignLine("sign.line2", "§f%player%", playerName, id));
        sign.setLine(2, formatSignLine("sign.line3", "§7Coffre #%id%", playerName, id));
        sign.setLine(3, formatSignLine("sign.line4", MoodStyle.BRAND, playerName, id));
        sign.setGlowingText(plugin.getConfig().getBoolean("sign.glow", true));
        sign.update(true, false);
    }

    private String formatSignLine(String path, String fallback, String playerName, int id) {
        return plugin.getConfig().getString(path, fallback).replace("%player%", playerName).replace("%id%", String.valueOf(id));
    }

    private SignPlace findSignPlace(Block chest, Player player) {
        BlockFace face = getTargetFace(chest, player);
        if (face == null) return null;
        Block signBlock = chest.getRelative(face);
        if (signBlock.getType() != Material.AIR) return null;
        return new SignPlace(signBlock, face);
    }

    private BlockFace getTargetFace(Block chest, Player player) {
        RayTraceResult result = player.rayTraceBlocks(5.0D);
        if (result != null && result.getHitBlock() != null && result.getHitBlock().equals(chest) && isHorizontal(result.getHitBlockFace())) return result.getHitBlockFace();

        double dx = player.getLocation().getX() - chest.getLocation().getX();
        double dz = player.getLocation().getZ() - chest.getLocation().getZ();
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? BlockFace.EAST : BlockFace.WEST;
        return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private boolean isHorizontal(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.EAST || face == BlockFace.WEST;
    }

    private record SignPlace(Block block, BlockFace wallFacing) {}
}
