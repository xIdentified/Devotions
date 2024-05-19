package me.xidentified.devotions;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private final Map<String, Miracle> miraclesMap = new HashMap<>();
    private YamlConfiguration soundsConfig;
    private YamlConfiguration savedItemsConfig;
    private YamlConfiguration deitiesConfig;
    private YamlConfiguration ritualConfig;
    private File savedItemsConfigFile;
    @Setter
    private boolean hideFavorMessages;

    public int getShrineLimit() {
        return plugin.getConfig().getInt("shrine-limit", 3);
    }

    public boolean resetFavorOnAbandon() {
        return plugin.getConfig().getBoolean("reset-favor-on-abandon", true);
    }

    public DevotionsConfig(Devotions plugin) {
        this.plugin = plugin;
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        reloadRitualConfig();
        reloadSoundsConfig();
        plugin.loadLanguages();

        // Reset the DevotionManager
        if (plugin.getDevotionManager() != null) {
            plugin.getDevotionManager().reset();
        }

        plugin.initializePlugin();
    }

    // Translates item ID for rituals and offerings
    // TODO: Saved item support
    public String getItemId(ItemStack item) {
        plugin.debugLog("Checking for vanilla item key: VANILLA:" + item.getType().name());
        // If the item is a potion, append the potion type to the ID
        if (item.getType() == Material.POTION) {
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
            if (potionMeta != null) {
                PotionData potionData = potionMeta.getBasePotionData();
                return "VANILLA:POTION_" + potionData.getType().name();
            }
        }
        // constructs vanilla item IDs for non-potion items
        return "VANILLA:" + item.getType().name();
    }

    public void loadRitualConfig() {
        File ritualFile = new File(plugin.getDataFolder(), "rituals.yml");
        if (!ritualFile.exists()) {
            plugin.saveResource("rituals.yml", false);
        }
        ritualConfig = YamlConfiguration.loadConfiguration(ritualFile);
    }

    private void reloadRitualConfig() {
        File ritualFile = new File(plugin.getDataFolder(), "rituals.yml");
        if (ritualFile.exists()) {
            ritualConfig = YamlConfiguration.loadConfiguration(ritualFile);
        }
        loadRituals();
    }

    public void loadRituals() {
        ConfigurationSection ritualsSection = ritualConfig.getConfigurationSection("rituals");
        if (ritualsSection == null) {
            plugin.getLogger().warning("No rituals section found in config.");
            return;
        }

        Set<String> usedItems = new HashSet<>();

        for (String key : ritualsSection.getKeys(false)) {
            try {
                String path = "rituals." + key + ".";

                // Check for duplicate items and warn
                String itemString = ritualConfig.getString(path + "item");
                if (itemString != null && !usedItems.add(itemString.toLowerCase())) {
                    plugin.getLogger().warning(
                            "Duplicate item detected for ritual: " + key + ". Use unique items for each ritual.");
                }

                // Parse general info
                String displayName = ritualConfig.getString(path + "display_name");
                String description = ritualConfig.getString(path + "description");
                Boolean consumeItem = ritualConfig.getBoolean(path + "consume-item", true);
                int favorReward = ritualConfig.getInt(path + "favor");

                RitualItem ritualItem = null;
                if (itemString != null) {
                    String[] parts = itemString.split(":");
                    if (parts.length == 2) {
                        String type = parts[0];
                        String itemId = parts[1];

                        if ("SAVED".equalsIgnoreCase(type)) {
                            ItemStack savedItem = loadSavedItem(itemId);
                            if (savedItem == null) {
                                plugin.getLogger().warning("Saved item not found: " + itemId + " for ritual: " + key);
                            } else {
                                ritualItem = new RitualItem("SAVED", savedItem);
                            }
                        } else if ("VANILLA".equalsIgnoreCase(type)) {
                            ritualItem = new RitualItem("VANILLA", itemId);
                        } else {
                            plugin.getLogger().warning("Unknown item type: " + type + " for ritual: " + key);
                        }
                    }
                }

                // Parse conditions
                String expression = ritualConfig.getString(path + "conditions.expression", "");
                String time = ritualConfig.getString(path + "conditions.time");
                String biome = ritualConfig.getString(path + "conditions.biome");
                String weather = ritualConfig.getString(path + "conditions.weather");
                String moonPhase = ritualConfig.getString(path + "conditions.moon_phase");
                double minAltitude = ritualConfig.getDouble(path + "conditions.min_altitude", 0.0);
                int minExperience = ritualConfig.getInt(path + "conditions.min_experience", 0);
                double minHealth = ritualConfig.getDouble(path + "conditions.min_health", 0.0);
                int minHunger = ritualConfig.getInt(path + "conditions.min_hunger", 0);

                RitualConditions ritualConditions = new RitualConditions(expression, time, biome, weather, moonPhase,
                        minAltitude, minExperience, minHealth, minHunger);

                // Parse outcome
                List<String> outcomeCommands;
                if (ritualConfig.isList(path + "outcome-command")) {
                    outcomeCommands = ritualConfig.getStringList(path + "outcome-command");
                } else {
                    String singleCommand = ritualConfig.getString(path + "outcome-command");
                    if (singleCommand == null || singleCommand.isEmpty()) {
                        plugin.getLogger().warning("No outcome specified for ritual: " + key);
                        continue; // Skip if no command is provided
                    }
                    outcomeCommands = Collections.singletonList(singleCommand);
                }
                RitualOutcome ritualOutcome = new RitualOutcome("RUN_COMMAND", outcomeCommands);

                // Parse objectives
                List<RitualObjective> objectives = new ArrayList<>();
                try {
                    List<Map<?, ?>> objectivesList = ritualConfig.getMapList(path + "objectives");
                    for (Map<?, ?> objectiveMap : objectivesList) {
                        String typeStr = (String) objectiveMap.get("type");
                        RitualObjective.Type type = RitualObjective.Type.valueOf(typeStr);
                        String objDescription = (String) objectiveMap.get("description");
                        String target = (String) objectiveMap.get("target");
                        int count = (Integer) objectiveMap.get("count");

                        RitualObjective objective = new RitualObjective(plugin, type, objDescription, target, count);
                        objectives.add(objective);
                        plugin.debugLog("Loaded objective " + objDescription + " for ritual " + key);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load objectives for ritual: " + key);
                    e.printStackTrace();
                }

                // Create and store the ritual
                Ritual ritual = new Ritual(plugin, key, displayName, description, ritualItem, consumeItem, favorReward,
                        ritualConditions, ritualOutcome, objectives);
                RitualManager.getInstance(plugin).addRitual(key, ritual);  // Store the ritual and key
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load ritual with key: " + key);
                e.printStackTrace();
            }
        }
    }

    private void reloadSoundsConfig() {
        File soundFile = new File(plugin.getDataFolder(), "sounds.yml");
        if (soundFile.exists()) {
            soundsConfig = YamlConfiguration.loadConfiguration(soundFile);
        }
        loadRituals();
    }

    public void reloadSavedItemsConfig() {
        if (savedItemsConfigFile == null) {
            savedItemsConfigFile = new File(plugin.getDataFolder(), "savedItems.yml");
        }
        savedItemsConfig = YamlConfiguration.loadConfiguration(savedItemsConfigFile);

        // Look for defaults in the jar
        InputStream defConfigStream = plugin.getResource("savedItems.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            savedItemsConfig.setDefaults(defConfig);
        }
    }

    public Map<String, Deity> loadDeities(YamlConfiguration deitiesConfig) {
        Map<String, Deity> deityMap = new HashMap<>();
        ConfigurationSection deitiesSection = deitiesConfig.getConfigurationSection("deities");
        if (deitiesSection == null) {
            plugin.getLogger().warning("No 'deities' section found in config.");
            return deityMap;
        }

        for (String deityKey : deitiesSection.getKeys(false)) {
            ConfigurationSection deityConfig = deitiesSection.getConfigurationSection(deityKey);
            if (deityConfig == null) {
                plugin.getLogger().warning("Deity configuration section is missing for: " + deityKey);
                continue;
            }

            String name = deityConfig.getString("name", deityKey); // Default to key if name is missing
            String lore = deityConfig.getString("lore", "");
            String domain = deityConfig.getString("domain", "");
            String alignment = deityConfig.getString("alignment", "");
            List<String> favoredRituals = deityConfig.getStringList("rituals");
            String abandonCondition = deityConfig.getString("abandon-condition", null);
            String selectionCondition = deityConfig.getString("selection-condition", null);

            // Load offerings
            List<String> offeringStrings = deityConfig.getStringList("offerings");
            List<Offering> favoredOfferings = offeringStrings.stream()
                    .map(offeringString -> parseOffering(offeringString,
                            deityKey)) // Use a separate method to parse each offering
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Parse blessings
            List<Blessing> deityBlessings = deityConfig.getStringList("blessings").stream()
                    .map(this::parseBlessing)
                    .collect(Collectors.toList());

            // Parse curses
            List<Curse> deityCurses = deityConfig.getStringList("curses").stream()
                    .map(this::parseCurse)
                    .collect(Collectors.toList());

            List<Miracle> deityMiracles = new ArrayList<>();

            for (String miracleString : deityConfig.getStringList("miracles")) {
                Miracle miracle = parseMiracle(miracleString);
                if (miracle != null) {
                    deityMiracles.add(miracle);
                    miraclesMap.put(miracleString, miracle);
                } else {
                    plugin.debugLog("Failed to parse miracle: " + miracleString + " for deity " + deityKey);
                }
            }

            Deity deity = new Deity(plugin, name, lore, domain, alignment, favoredOfferings, favoredRituals,
                    deityBlessings, deityCurses, deityMiracles, abandonCondition, selectionCondition);
            deityMap.put(deityKey.toLowerCase(), deity);
            plugin.getLogger()
                    .info("Loaded deity " + deity.getName() + " with " + favoredOfferings.size() + " offerings.");
        }

        return deityMap;
    }

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
        double chance = 1.0; // Default chance is 100%

        try {
            favorValue = Integer.parseInt(favorAndChance[0]);
            if (favorAndChance.length > 1) {
                chance = Double.parseDouble(favorAndChance[1]);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger()
                    .warning("Invalid favor value or chance in offerings for deity " + deityKey + ": " + parts[2]);
            return null;
        }

        List<String> commands = new ArrayList<>();
        if (parts.length > 3) {
            commands = Arrays.asList(parts[3].split(";"));
        }

        ItemStack itemStack = resolveItemStack(type, itemId, deityKey);
        if (itemStack == null) {
            // resolveItemStack will log the error
            return null;
        }

        return new Offering(itemStack, favorValue, commands, chance);
    }

    private ItemStack resolveItemStack(String type, String itemId, String deityKey) {
        if ("Saved".equalsIgnoreCase(type)) {
            ItemStack itemStack = loadSavedItem(itemId);
            if (itemStack == null) {
                plugin.getLogger().warning("Saved item not found: " + itemId + " for deity: " + deityKey);
            }
            return itemStack;
        } else {
            Material material = Material.matchMaterial(itemId);
            if (material != null) {
                return new ItemStack(material);
            } else {
                plugin.getLogger().warning("Invalid material in offerings for deity " + deityKey + ": " + itemId);
                return null;
            }
        }
    }

    public Map<String, Deity> reloadDeitiesConfig() {
        File deitiesFile = new File(plugin.getDataFolder(), "deities.yml");
        if (!deitiesFile.exists()) {
            plugin.saveResource("deities.yml", false);
        }

        if (deitiesFile.exists()) {
            deitiesConfig = YamlConfiguration.loadConfiguration(deitiesFile);
            return loadDeities(deitiesConfig);
        } else {
            plugin.getLogger().severe("Unable to create default deities.yml");
            return new HashMap<>(); // Return an empty map as a fallback
        }
    }

    public Miracle parseMiracle(String miracleString) {
        plugin.debugLog("Parsing miracle: " + miracleString);
        MiracleEffect effect;
        List<Condition> conditions = new ArrayList<>();

        String[] parts = miracleString.split(":", 2);
        String miracleType = parts[0];  // Define miracleType here

        plugin.debugLog("Parsed miracleString: " + miracleString);
        plugin.debugLog("Miracle type: " + miracleType);
        if (parts.length > 1) {
            plugin.debugLog("Command/Argument: " + parts[1]);
        }

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
                effect = new SummonAidEffect(3); // Summoning 3 entities.
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
                        plugin.debugLog("Invalid duration provided for double_crops miracle.");
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
                plugin.debugLog("Unrecognized miracle encountered in parseMiracle!");
                return null;
            }
        }

        return new Miracle(miracleType, conditions, effect);
    }

    private Blessing parseBlessing(String blessingString) {
        String[] parts = blessingString.split(",");
        PotionEffectType effect = PotionEffectType.getByName(parts[0]);
        int strength = Integer.parseInt(parts[1]);
        int duration = Integer.parseInt(parts[2]);
        return new Blessing(parts[0], duration, strength, effect);
    }

    private Curse parseCurse(String curseString) {
        String[] parts = curseString.split(",");
        PotionEffectType effect = PotionEffectType.getByName(parts[0]);
        int strength = Integer.parseInt(parts[1]);
        int duration = Integer.parseInt(parts[2]);
        return new Curse(parts[0], duration, strength, effect);
    }


    public void loadSoundsConfig() {
        File soundsFile = new File(plugin.getDataFolder(), "sounds.yml");
        if (!soundsFile.exists()) {
            plugin.saveResource("sounds.yml", false);
        }
        soundsConfig = YamlConfiguration.loadConfiguration(soundsFile);
        plugin.debugLog("sounds.yml successfully loaded!");
    }

    private ItemStack loadSavedItem(String name) {
        File storageFolder = new File(plugin.getDataFolder(), "storage");
        File itemsFile = new File(storageFolder, "savedItems.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);

        if (config.contains("items." + name)) {
            return ItemStack.deserialize(config.getConfigurationSection("items." + name).getValues(false));
        }
        return null;
    }


}