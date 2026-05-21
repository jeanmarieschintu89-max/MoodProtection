#!/usr/bin/env bash
set -euo pipefail

ROOT="$(pwd)"
LOCK="$ROOT/MoodcraftChestLock-main"
TRADE="$ROOT/MoodVillagerTradeBlocker-main"

if [ ! -d "$LOCK/src/main/java" ]; then
  echo "Dossier manquant: MoodcraftChestLock-main/src/main/java"
  exit 1
fi
if [ ! -d "$TRADE/src/main/java" ]; then
  echo "Dossier manquant: MoodVillagerTradeBlocker-main/src/main/java"
  exit 1
fi

echo "== MoodProtection : fusion propre =="
mkdir -p src/main/java src/main/resources

cp -R "$LOCK/src/main/java/"* src/main/java/
cp -R "$TRADE/src/main/java/"* src/main/java/

python3 <<'PY'
from pathlib import Path

moodlock = Path('src/main/java/fr/moodcraft/lock/MoodLock.java')
text = moodlock.read_text()
text = text.replace('import fr.moodcraft.lock.manager.LockManager;\n', 'import fr.moodcraft.lock.manager.LockManager;\nimport fr.moodcraft.tradeblocker.TradeFilter;\nimport fr.moodcraft.tradeblocker.VillagerTradeListener;\n')
text = text.replace('    private LockContentManager contentManager;\n', '    private LockContentManager contentManager;\n    private TradeFilter tradeFilter;\n')
text = text.replace('        contentManager = new LockContentManager(this, lockManager);\n        contentManager.load();\n', '        contentManager = new LockContentManager(this, lockManager);\n        contentManager.load();\n\n        tradeFilter = new TradeFilter(this);\n')
text = text.replace('        registerCommand("lockadmin", new LockAdminCommand(lockManager, contentManager));\n', '        registerCommand("lockadmin", new LockAdminCommand(lockManager, contentManager));\n        registerCommand("tradeblockerreload", (sender, command, label, args) -> {\n            reloadConfig();\n            tradeFilter.reload();\n            sender.sendMessage("§8----- §6✦ §aMood§6Craft §fProtection ✦ §8-----");\n            sender.sendMessage("§a✔ §fConfiguration protection rechargée.");\n            sender.sendMessage("§8-----------------------------");\n            return true;\n        });\n')
text = text.replace('        getServer().getPluginManager().registerEvents(new LockProtectionListener(lockManager), this);\n', '        getServer().getPluginManager().registerEvents(new LockProtectionListener(lockManager), this);\n        getServer().getPluginManager().registerEvents(new VillagerTradeListener(this, tradeFilter), this);\n')
text = text.replace('        getLogger().info("MoodLock actif.");', '        getLogger().info("MoodProtection actif.");')
text = text.replace('        getLogger().info("MoodLock désactivé.");', '        getLogger().info("MoodProtection désactivé.");')
moodlock.write_text(text)

trade_filter = Path('src/main/java/fr/moodcraft/tradeblocker/TradeFilter.java')
text = trade_filter.read_text()
text = text.replace('private final MoodVillagerTradeBlocker plugin;', 'private final org.bukkit.plugin.java.JavaPlugin plugin;')
text = text.replace('public TradeFilter(MoodVillagerTradeBlocker plugin)', 'public TradeFilter(org.bukkit.plugin.java.JavaPlugin plugin)')
trade_filter.write_text(text)

listener = Path('src/main/java/fr/moodcraft/tradeblocker/VillagerTradeListener.java')
text = listener.read_text()
text = text.replace('private final MoodVillagerTradeBlocker plugin;', 'private final org.bukkit.plugin.java.JavaPlugin plugin;')
text = text.replace('public VillagerTradeListener(MoodVillagerTradeBlocker plugin, TradeFilter filter)', 'public VillagerTradeListener(org.bukkit.plugin.java.JavaPlugin plugin, TradeFilter filter)')
listener.write_text(text)
PY

cat > src/main/resources/plugin.yml <<'EOF'
name: MoodProtection
version: 1.0
main: fr.moodcraft.lock.MoodLock
api-version: '1.21'
author: MoodCraft
description: Protections MoodCraft : coffres verrouillés et filtrage des trades villageois

commands:
  lock:
    description: Verrouiller un coffre
  unlock:
    description: Déverrouiller son coffre
  adminunlock:
    description: Déverrouiller un coffre de force
  locktp:
    description: Se téléporter aux coffres verrouillés
  lockadd:
    description: Ajouter un membre au coffre
  lockdel:
    description: Retirer un membre du coffre
  lockadmin:
    description: Administration avancée des coffres verrouillés
    usage: /lockadmin <status|info|list|content|history|tp|transfer|purge|cleanup|save|reload>
    permission: moodlock.admin
    permission-message: "§c✖ §fAccès réservé à l'administration des coffres."
  tradeblockerreload:
    description: Recharge la configuration du filtre des échanges villageois
    usage: /tradeblockerreload
    permission: moodtradeblocker.admin
    permission-message: "§c✖ §fAccès réservé à l'administration."
    aliases:
      - mtbreload
      - villagertradereload

permissions:
  moodlock.vip:
    default: false
  moodlock.admin:
    default: op
  moodlock.adminunlock:
    default: op
  moodlock.locktp:
    default: op
  moodtradeblocker.admin:
    description: Administration du filtre des échanges villageois
    default: op
EOF

cat > src/main/resources/config.yml <<'EOF'
# MoodProtection Config
# Fusion de MoodLock et MoodVillagerTradeBlocker.

# Monde où les joueurs peuvent utiliser /lock et /unlock.
world: world

limits:
  member: 5
  vip: 15

sign:
  material: BIRCH_WALL_SIGN
  glow: true
  line1: "§c✦ SÉCURISÉ ✦"
  line2: "§f%player%"
  line3: "§7Coffre #%id%"
  line4: "§aMood§6Craft"

settings:
  notify-admins: false
  remove-empty-villagers: false
  block-all-emerald-results: true

blocked-result-items:
  - EMERALD_BLOCK
  - DIAMOND
  - DIAMOND_BLOCK
  - DIAMOND_SWORD
  - DIAMOND_PICKAXE
  - DIAMOND_AXE
  - DIAMOND_SHOVEL
  - DIAMOND_HOE
  - DIAMOND_HELMET
  - DIAMOND_CHESTPLATE
  - DIAMOND_LEGGINGS
  - DIAMOND_BOOTS
  - IRON_INGOT
  - IRON_BLOCK
  - GOLD_INGOT
  - GOLD_BLOCK
  - COPPER_INGOT
  - COPPER_BLOCK
  - REDSTONE
  - REDSTONE_BLOCK
  - LAPIS_LAZULI
  - LAPIS_BLOCK
  - QUARTZ
  - COAL
  - COAL_BLOCK
  - AMETHYST_SHARD
  - NETHERITE_INGOT
  - NETHERITE_SCRAP
  - NETHERITE_UPGRADE_SMITHING_TEMPLATE

blocked-emerald-ingredient-items:
  - STICK
  - PAPER
  - ROTTEN_FLESH
  - WHEAT
  - WHEAT_SEEDS
  - CARROT
  - POTATO
  - BEETROOT
  - BEETROOT_SEEDS
  - PUMPKIN
  - MELON
  - MELON_SLICE
  - SWEET_BERRIES
  - STRING
  - FEATHER
  - LEATHER
  - RABBIT_HIDE
  - FLINT
  - CLAY_BALL
  - STONE
  - GRANITE
  - DIORITE
  - ANDESITE
  - GLASS_PANE
  - COAL
  - CHARCOAL
  - IRON_INGOT
  - GOLD_INGOT
  - RAW_IRON
  - RAW_GOLD
  - RAW_COPPER
  - COD
  - SALMON
  - TROPICAL_FISH
  - PUFFERFISH
  - WHITE_WOOL
  - ORANGE_WOOL
  - MAGENTA_WOOL
  - LIGHT_BLUE_WOOL
  - YELLOW_WOOL
  - LIME_WOOL
  - PINK_WOOL
  - GRAY_WOOL
  - LIGHT_GRAY_WOOL
  - CYAN_WOOL
  - PURPLE_WOOL
  - BLUE_WOOL
  - BROWN_WOOL
  - GREEN_WOOL
  - RED_WOOL
  - BLACK_WOOL

expensive-enchanted-books:
  MENDING: 64
  FORTUNE: 56
  SILK_TOUCH: 48
  EFFICIENCY: 48
  UNBREAKING: 48
  LOOTING: 56
EOF

cat > pom.xml <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>fr.moodcraft</groupId>
    <artifactId>MoodProtection</artifactId>
    <version>1.0</version>
    <packaging>jar</packaging>

    <name>MoodProtection</name>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <repositories>
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.21.1-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>MoodProtection</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <release>21</release>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

rm -rf MoodcraftChestLock-main MoodVillagerTradeBlocker-main
rm -f MoodcraftChestLock-main.zip MoodVillagerTradeBlocker-main.zip

echo "== Fusion MoodProtection terminée =="
echo "Lance : mvn clean package"
