package fr.moodcraft.lock;

import fr.moodcraft.lock.command.AdminUnlockCommand;
import fr.moodcraft.lock.command.LockAddCommand;
import fr.moodcraft.lock.command.LockAdminCommand;
import fr.moodcraft.lock.command.LockCommand;
import fr.moodcraft.lock.command.LockDelCommand;
import fr.moodcraft.lock.command.LockTpCommand;
import fr.moodcraft.lock.command.UnlockCommand;
import fr.moodcraft.lock.listener.LockListener;
import fr.moodcraft.lock.listener.LockProtectionListener;
import fr.moodcraft.lock.manager.LockContentManager;
import fr.moodcraft.lock.manager.LockManager;
import fr.moodcraft.tradeblocker.TradeFilter;
import fr.moodcraft.tradeblocker.VillagerTradeListener;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MoodLock extends JavaPlugin {

    public static MoodLock instance;

    private LockManager lockManager;
    private LockContentManager contentManager;
    private TradeFilter tradeFilter;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        lockManager = new LockManager(this);
        lockManager.load();

        contentManager = new LockContentManager(this, lockManager);
        contentManager.load();

        tradeFilter = new TradeFilter(this);

        registerCommand("lock", new LockCommand(this, lockManager, contentManager));
        registerCommand("unlock", new UnlockCommand(this, lockManager, contentManager));
        registerCommand("adminunlock", new AdminUnlockCommand(this, lockManager));
        registerCommand("locktp", new LockTpCommand(this, lockManager));
        registerCommand("lockadd", new LockAddCommand(this, lockManager));
        registerCommand("lockdel", new LockDelCommand(this, lockManager));
        registerCommand("lockadmin", new LockAdminCommand(lockManager, contentManager));
        registerCommand("tradeblockerreload", (sender, command, label, args) -> {
            reloadConfig();
            tradeFilter.reload();
            sender.sendMessage("§8----- §6✦ §aMood§6Craft §fProtection ✦ §8-----");
            sender.sendMessage("§a✔ §fConfiguration protection rechargée.");
            sender.sendMessage("§8-----------------------------");
            return true;
        });

        getServer().getPluginManager().registerEvents(new LockListener(lockManager, contentManager), this);
        getServer().getPluginManager().registerEvents(new LockProtectionListener(lockManager), this);
        getServer().getPluginManager().registerEvents(new VillagerTradeListener(this, tradeFilter), this);

        getServer().getScheduler().runTaskLater(this, this::snapshotExistingLocks, 40L);
        getLogger().info("MoodProtection actif.");
    }

    @Override
    public void onDisable() {
        if (lockManager != null) lockManager.save();
        if (contentManager != null) contentManager.save();
        getLogger().info("MoodProtection désactivé.");
    }

    private void snapshotExistingLocks() {
        if (lockManager == null || contentManager == null) return;
        int scanned = 0;
        for (Location location : lockManager.getLockedLocations()) {
            if (location == null || location.getWorld() == null) continue;
            scanned++;
            contentManager.snapshot(location.getBlock(), "startup");
        }
        getLogger().info("[LOCK-CONTENT] Scan démarrage terminé. Coffres verrouillés vérifiés: " + scanned);
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Commande absente du plugin.yml : " + name);
            return;
        }
        command.setExecutor(executor);
    }

    public LockManager getLockManager() {
        return lockManager;
    }
}
