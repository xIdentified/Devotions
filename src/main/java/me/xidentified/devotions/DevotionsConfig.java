package me.xidentified.devotions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import me.xidentified.devotions.effects.Blessing;
import me.xidentified.devotions.effects.Curse;
import me.xidentified.devotions.rituals.RitualManager;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.rituals.RitualConditions;
import me.xidentified.devotions.rituals.RitualItem;
import me.xidentified.devotions.rituals.RitualObjective;
import me.xidentified.devotions.rituals.RitualOutcome;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;

@Getter
public class DevotionsConfig {

    private final Devotions plugin;

    /**
     * -- GETTER --
     * If you need direct access to the main config.
     */
    // Main config (config.yml), already managed by plugin.getConfig(), but we'll store a reference
    @Getter
    private FileConfiguration mainConfig;

    /**
     * -- GETTER --
     * If you want direct access to sounds config for playing SFX, etc.
     */
    // Separate YAML configs
    @Getter
    private YamlConfiguration soundsConfig;
    private YamlConfiguration deitiesConfig;
    private YamlConfiguration ritualConfig;

    // Whether to hide favor messages; read from config.yml (or you can store directly in plugin)
    @Getter
    @Setter
    private boolean hideFavorMessages;

    // Miracles map if you need to track them globally (populated in parseMiracle)
    private final Map<String, Miracle> miraclesMap = new HashMap<>();

    /**
     * Constructor.
     * We won't load configs here to avoid partial data if plugin isn't fully initialized yet.
     */
    public DevotionsConfig(Devotions plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------
    //  1) INITIALIZE - Called once during plugin startup
    // -------------------------------------------------------
    public void initConfigs() {
        // 1) Make sure the plugin’s default config.yml is created if missing
        plugin.saveDefaultConfig();
        // 2) Reload plugin config from disk -> plugin.getConfig()
        plugin.reloadConfig();
        this.mainConfig = plugin.getConfig();

        // You can read any top-level config settings here, e.g.:
        this.hideFavorMessages = mainConfig.getBoolean("hide-favor-messages", false);

        // 3) Load each custom YAML
        loadRitualConfig(false);
        loadSoundsConfig(false);
        loadDeitiesConfig(false);

        // 4) Parse data from the loaded files into memory
        loadRituals(); // parse "rituals.yml"
        // If you want to parse deities immediately, do something like:
        // Map<String, Deity> loadedDeities = loadDeities(deitiesConfig);
        // plugin.setDeities(loadedDeities) or something similar.

        plugin.debugLog("All configs have been initialized.");
    }

    // -------------------------------------------------------
    //  2) RELOAD - Called if user does "/devotions reload", etc.
    // -------------------------------------------------------
    public void reloadConfigs() {
        plugin.reloadConfig();
        this.mainConfig = plugin.getConfig();
        this.hideFavorMessages = mainConfig.getBoolean("hide-favor-messages", false);

        loadRitualConfig(true);
        loadSoundsConfig(true);
        loadDeitiesConfig(true);

        // 3) Re-parse
        loadRituals();
        // e.g. re-parse deities:
        // Map<String, Deity> reloadedDeities = loadDeities(deitiesConfig);
        // plugin.setDeities(reloadedDeities);

        plugin.loadLanguages();

        if (plugin.getDevotionManager() != null) {
            plugin.getDevotionManager().reset();
        }

        plugin.initializePlugin();

        plugin.debugLog("All configs have been reloaded.");
    }

    public Map<String, Deity> reloadDeitiesConfig() {
        // Reload the file on disk
        loadDeitiesConfig(true);
        // Now parse the reloaded file into a Map<String, Deity>
        return loadDeities(this.deitiesConfig);
    }

    // -------------------------------------------------------
    //  LOAD / RELOAD RITUAL CONFIG
    // -------------------------------------------------------
    public void loadRitualConfig(boolean overwrite) {
        File ritualFile = new File(plugin.getDataFolder(), "rituals.yml");
        if (!ritualFile.exists()) {
            // creates default from jar if not found
            plugin.saveResource("rituals.yml", false);
        }
        // "overwrite" can be used if we want to forcibly overwrite
        // existing local changes with jar defaults. Usually we do false.
        this.ritualConfig = YamlConfiguration.loadConfiguration(ritualFile);
    }

    public void loadRituals() {
        // We assume ritualConfig is not null if we've loaded it above
        if (ritualConfig == null) {
            plugin.getLogger().warning("ritualConfig is null; cannot load rituals.");
            return;
        }

        ConfigurationSection ritualsSection = ritualConfig.getConfigurationSection("rituals");
        if (ritualsSection == null) {
            plugin.getLogger().warning("No 'rituals' section found in rituals.yml.");
            return;
        }

        // If you store these rituals in a manager, clear old data first
        RitualManager.getInstance(plugin).clearRituals();

        Set<String> usedItems = new HashSet<>();

        for (String key : ritualsSection.getKeys(false)) {
            try {
                String path = "rituals." + key + ".";
                String itemString = ritualConfig.getString(path + "item");
                if (itemString != null && !usedItems.add(itemString.toLowerCase())) {
                    plugin.getLogger().warning(
                            "Duplicate item detected for ritual: " + key +
                                    ". Use unique items for each ritual.");
                }

                // parse out the info
                String displayName = ritualConfig.getString(path + "display_name");
                String description = ritualConfig.getString(path + "description");
                boolean consumeItem = ritualConfig.getBoolean(path + "consume-item", true);
                int favorReward = ritualConfig.getInt(path + "favor");

                // parse the item (SAVED or VANILLA)
                RitualItem ritualItem = parseRitualItem(itemString, key);

                // parse conditions
                String expression = ritualConfig.getString(path + "conditions.expression", "");
                String time = ritualConfig.getString(path + "conditions.time");
                String biome = ritualConfig.getString(path + "conditions.biome");
                String weather = ritualConfig.getString(path + "conditions.weather");
                String moonPhase = ritualConfig.getString(path + "conditions.moon_phase");
                double minAltitude = ritualConfig.getDouble(path + "conditions.min_altitude", 0.0);
                int minExp = ritualConfig.getInt(path + "conditions.min_experience", 0);
                double minHealth = ritualConfig.getDouble(path + "conditions.min_health", 0.0);
                int minHunger = ritualConfig.getInt(path + "conditions.min_hunger", 0);

                RitualConditions conditions = new RitualConditions(
                        expression, time, biome, weather, moonPhase,
                        minAltitude, minExp, minHealth, minHunger
                );

                // parse outcome
                List<String> outcomeCommands = new ArrayList<>();
                if (ritualConfig.isList(path + "outcome-command")) {
                    outcomeCommands = ritualConfig.getStringList(path + "outcome-command");
                } else {
                    String singleCommand = ritualConfig.getString(path + "outcome-command");
                    if (singleCommand == null || singleCommand.isEmpty()) {
                        plugin.getLogger().warning("No outcome specified for ritual: " + key);
                        continue;
                    }
                    outcomeCommands = Collections.singletonList(singleCommand);
                }
                RitualOutcome outcome = new RitualOutcome("RUN_COMMAND", outcomeCommands);

                // parse objectives
                List<RitualObjective> objectives = parseObjectives(path, key);

                // Create the Ritual object
                Ritual ritual = new Ritual(
                        plugin,
                        key,
                        displayName,
                        description,
                        ritualItem,
                        consumeItem,
                        favorReward,
                        conditions,
                        outcome,
                        objectives
                );

                // Finally, store the ritual
                RitualManager.getInstance(plugin).addRitual(key, ritual);

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load ritual with key: " + key);
                e.printStackTrace();
            }
        }
    }

    // Helper to parse itemString from "rituals.yml"
    private RitualItem parseRitualItem(String itemString, String ritualKey) {
        if (itemString == null) return null;
        String[] parts = itemString.split(":");
        if (parts.length == 2) {
            String type = parts[0];
            String itemId = parts[1];
            if ("VANILLA".equalsIgnoreCase(type)) {
                return new RitualItem("VANILLA", itemId);
            } else {
                plugin.getLogger().warning("Unknown item type: " + type + " for ritual: " + ritualKey);
            }
        }
        return null;
    }

    // Helper to parse objectives from the config
    private List<RitualObjective> parseObjectives(String path, String ritualKey) {
        List<RitualObjective> objectives = new ArrayList<>();
        try {
            List<Map<?, ?>> objList = ritualConfig.getMapList(path + "objectives");
            for (Map<?, ?> objMap : objList) {
                String typeStr = (String) objMap.get("type");
                RitualObjective.Type type = RitualObjective.Type.valueOf(typeStr);

                String description = (String) objMap.get("description");
                String target = (String) objMap.get("target");
                int count = (Integer) objMap.get("count");

                // If target isn't coordinates, interpret as region
                boolean isRegionTarget = !target.matches("-?\\d+,-?\\d+,-?\\d+");

                RitualObjective objective = new RitualObjective(plugin, type, description, target, count, isRegionTarget);
                objectives.add(objective);
                plugin.debugLog("Loaded objective " + description + " for ritual.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load objectives for ritual: " + ritualKey);
            e.printStackTrace();
        }
        return objectives;
    }

    // -------------------------------------------------------
    //  LOAD / RELOAD SOUNDS.YML
    // -------------------------------------------------------
    private void loadSoundsConfig(boolean overwrite) {
        File soundsFile = new File(plugin.getDataFolder(), "sounds.yml");
        if (!soundsFile.exists()) {
            plugin.saveResource("sounds.yml", false);
        }
        this.soundsConfig = YamlConfiguration.loadConfiguration(soundsFile);
        plugin.debugLog("sounds.yml successfully loaded!");
    }

    // -------------------------------------------------------
    //  TODO: LOAD / RELOAD SAVEDITEMS.YML
    // -------------------------------------------------------



    // -------------------------------------------------------
    //  LOAD / RELOAD DEITIES.YML
    // -------------------------------------------------------
    private void loadDeitiesConfig(boolean overwrite) {
        File deitiesFile = new File(plugin.getDataFolder(), "deities.yml");
        if (!deitiesFile.exists()) {
            plugin.saveResource("deities.yml", false);
        }
        this.deitiesConfig = YamlConfiguration.loadConfiguration(deitiesFile);
    }

    // Called from somewhere else, or after loadDeitiesConfig
    public Map<String, Deity> loadDeities(YamlConfiguration yaml) {
        Map<String, Deity> deityMap = new HashMap<>();
        ConfigurationSection deitiesSection = yaml.getConfigurationSection("deities");
        if (deitiesSection == null) {
            plugin.getLogger().warning("No 'deities' section found in config.");
            return deityMap;
        }

        for (String deityKey : deitiesSection.getKeys(false)) {
            ConfigurationSection deityCfg = deitiesSection.getConfigurationSection(deityKey);
            if (deityCfg == null) {
                plugin.getLogger().warning("Deity configuration section missing for: " + deityKey);
                continue;
            }

            String name = deityCfg.getString("name", deityKey);
            String lore = deityCfg.getString("lore", "");
            String domain = deityCfg.getString("domain", "");
            String alignment = deityCfg.getString("alignment", "");
            List<String> favoredRituals = deityCfg.getStringList("rituals");
            String abandonCondition = deityCfg.getString("abandon-condition", null);
            String selectionCondition = deityCfg.getString("selection-condition", null);

            // parse offerings
            List<String> offeringStrings = deityCfg.getStringList("offerings");
            List<Offering> favoredOfferings = offeringStrings.stream()
                    .map(off -> parseOffering(off, deityKey))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // parse blessings
            List<Blessing> blessings = deityCfg.getStringList("blessings").stream()
                    .map(this::parseBlessing)
                    .collect(Collectors.toList());

            // parse curses
            List<Curse> curses = deityCfg.getStringList("curses").stream()
                    .map(this::parseCurse)
                    .collect(Collectors.toList());

            // parse miracles
            List<Miracle> deityMiracles = new ArrayList<>();
            for (String miracleStr : deityCfg.getStringList("miracles")) {
                Miracle miracle = parseMiracle(miracleStr);
                if (miracle != null) {
                    deityMiracles.add(miracle);
                    // store in miraclesMap if you want a global reference
                    miraclesMap.put(miracleStr, miracle);
                } else {
                    plugin.debugLog("Failed to parse miracle: " + miracleStr + " for deity " + deityKey);
                }
            }

            // create deity
            Deity deity = new Deity(
                    plugin,
                    name,
                    lore,
                    domain,
                    alignment,
                    favoredOfferings,
                    favoredRituals,
                    blessings,
                    curses,
                    deityMiracles,
                    abandonCondition,
                    selectionCondition
            );
            deityMap.put(deityKey.toLowerCase(), deity);
            plugin.getLogger().info("Loaded deity " + name + " with " + favoredOfferings.size() + " offerings.");
        }
        return deityMap;
    }

    // -------------------------------------------------------
    // PARSE OFFERING, MIRACLE, BLESSING, CURSE
    // -------------------------------------------------------
    private Offering parseOffering(String offeringString, String deityKey) {
        String[] parts = offeringString.split(":");
        if (parts.length < 3) {
            plugin.getLogger().warning("Invalid offering format for deity " + deityKey + ": " + offeringString);
            return null;
        }

        String type = parts[0];
        String itemId = parts[1];
        String[] favorAndChance = parts[2].split("-");
        int favorValue;
        double chance = 1.0;

        try {
            favorValue = Integer.parseInt(favorAndChance[0]);
            if (favorAndChance.length > 1) {
                chance = Double.parseDouble(favorAndChance[1]);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger()
                    .warning("Invalid favor or chance in offering for deity " + deityKey + ": " + parts[2]);
            return null;
        }

        List<String> commands = new ArrayList<>();
        if (parts.length > 3) {
            commands = Arrays.asList(parts[3].split(";"));
        }

        ItemStack itemStack = resolveItemStack(type, itemId, deityKey);
        if (itemStack == null) {
            return null; // error logged already
        }

        return new Offering(itemStack, favorValue, commands, chance);
    }

    private ItemStack resolveItemStack(String type, String itemId, String deityKey) {
        Material material = Material.matchMaterial(itemId);
        if (material != null) {
            return new ItemStack(material);
        } else {
            plugin.getLogger().warning("Invalid material in offerings for deity " + deityKey + ": " + itemId);
            return null;
        }
    }

    public Miracle parseMiracle(String miracleString) {
        plugin.debugLog("Parsing miracle: " + miracleString);
        MiracleEffect effect;
        List<Condition> conditions = new ArrayList<>();

        String[] parts = miracleString.split(":", 2);
        if (parts.length == 0) {
            plugin.debugLog("Empty miracle string!");
            return null;
        }

        String miracleType = parts[0];
        plugin.debugLog("Miracle type: " + miracleType);

        switch (miracleType) {
            case "revive_on_death" -> {
                effect = new ReviveOnDeath();
                conditions.add(new IsDeadCondition());
            }
            case "stop_burning" -> {
                effect = new SaveFromBurning();
                conditions.add(new IsOnFireCondition());
            }
            case "repair_all" -> {
                effect = new RepairAllItems();
                conditions.add(new HasRepairableItemsCondition());
            }
            case "summon_aid" -> {
                effect = new SummonAidEffect(3);
                conditions.add(new LowHealthCondition());
                conditions.add(new NearHostileMobsCondition());
            }
            case "village_hero" -> {
                effect = new HeroEffectInVillage();
                conditions.add(new NearVillagersCondition());
            }
            case "double_crops" -> {
                if (parts.length > 1) {
                    try {
                        int duration = Integer.parseInt(parts[1]);
                        effect = new DoubleCropDropsEffect(plugin, duration);
                        conditions.add(new NearCropsCondition());
                    } catch (NumberFormatException e) {
                        plugin.debugLog("Invalid duration for double_crops miracle.");
                        return null;
                    }
                } else {
                    plugin.debugLog("No duration provided for double_crops miracle.");
                    return null;
                }
            }
            case "run_command" -> {
                if (parts.length > 1) {
                    String command = parts[1];
                    effect = new ExecuteCommandEffect(command);
                } else {
                    plugin.debugLog("No command provided for run_command miracle.");
                    return null;
                }
            }
            default -> {
                plugin.debugLog("Unrecognized miracle type: " + miracleType);
                return null;
            }
        }
        return new Miracle(miracleType, conditions, effect);
    }

    private Blessing parseBlessing(String blessingString) {
        String[] parts = blessingString.split(",");
        PotionEffectType effectType = PotionEffectType.getByName(parts[0]);
        int strength = Integer.parseInt(parts[1]);
        int duration = Integer.parseInt(parts[2]);
        return new Blessing(parts[0], duration, strength, effectType);
    }

    private Curse parseCurse(String curseString) {
        String[] parts = curseString.split(",");
        PotionEffectType effectType = PotionEffectType.getByName(parts[0]);
        int strength = Integer.parseInt(parts[1]);
        int duration = Integer.parseInt(parts[2]);
        return new Curse(parts[0], duration, strength, effectType);
    }

    // -------------------------------------------------------
    //  GETTERS
    // -------------------------------------------------------

    /**
     * Example: retrieve shrine-limit from config.yml.
     * If mainConfig is null or missing the key, default to 3.
     */
    public int getShrineLimit() {
        return mainConfig != null
                ? mainConfig.getInt("shrine-limit", 3)
                : 3;
    }

    public String getItemId(ItemStack item) {
        plugin.debugLog("Checking for vanilla item key: VANILLA:" + item.getType().name());

        // If the item is a potion, append potion type to the ID for uniqueness
        if (item.getType() == Material.POTION && item.getItemMeta() instanceof PotionMeta potionMeta) {
            PotionData potionData = potionMeta.getBasePotionData();
            return "VANILLA:POTION_" + potionData.getType().name();
        }

        // Otherwise, just use material name
        return "VANILLA:" + item.getType().name();
    }

}
