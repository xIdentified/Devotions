package me.xidentified.devotions;

import lombok.Getter;
import me.xidentified.devotions.effects.Blessing;
import me.xidentified.devotions.effects.Curse;
import me.xidentified.devotions.managers.RitualManager;
import me.xidentified.devotions.rituals.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class DevotionsConfig {
    private final Devotions plugin;
    private final Map<String, Miracle> miraclesMap = new HashMap<>();
    private YamlConfiguration soundsConfig;
    private YamlConfiguration savedItemsConfig;
    private YamlConfiguration deitiesConfig;
    private YamlConfiguration ritualConfig;
    private File savedItemsConfigFile;

    public int getShrineLimit() {
        return plugin.getConfig().getInt("shrine-limit", 3);
    }

    public DevotionsConfig(Devotions plugin) {
        this.plugin = plugin;
        reloadConfigs();
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

    public YamlConfiguration getDeitiesConfig() {
        if (deitiesConfig == null) {
            File deitiesFile = new File(plugin.getDataFolder(), "deities.yml");
            deitiesConfig = YamlConfiguration.loadConfiguration(deitiesFile);
        }
        return deitiesConfig;
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

        for (String key : ritualsSection.getKeys(false)) {
            try {
                String path = "rituals." + key + ".";

                // Parse general info
                String displayName = ritualConfig.getString(path + "display_name");
                String description = ritualConfig.getString(path + "description");
                int favorReward = ritualConfig.getInt(path + "favor");

                // Parse item
                String itemString = ritualConfig.getString(path + "item");
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
                Ritual ritual = new Ritual(plugin, displayName, description, ritualItem, favorReward, ritualConditions, ritualOutcome, objectives);
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

    public FileConfiguration getSavedItemsConfig() {
        if (savedItemsConfig == null) {
            reloadSavedItemsConfig();
        }
        return savedItemsConfig;
    }

    public Map<String, Deity> loadDeities(YamlConfiguration deitiesConfig) {
        Map<String, Deity> deityMap = new HashMap<>();
        ConfigurationSection deitiesSection = deitiesConfig.getConfigurationSection("deities");
        assert deitiesSection != null;
        for (String deityKey : deitiesSection.getKeys(false)) {
            ConfigurationSection deityConfig = deitiesSection.getConfigurationSection(deityKey);

            assert deityConfig != null;
            String name = deityConfig.getString("name");
            String lore = deityConfig.getString("lore");
            String domain = deityConfig.getString("domain");
            String alignment = deityConfig.getString("alignment");
            List<String> favoredRituals = deityConfig.getStringList("rituals");

            // Load offerings
            List<String> offeringStrings = deityConfig.getStringList("offerings");
            List<Offering> favoredOfferings = offeringStrings.stream()
                    .map(offeringString -> {
                        String[] parts = offeringString.split(":");
                        if (parts.length < 3) {
                            plugin.getLogger().warning("Invalid offering format for deity " + deityKey + ": " + offeringString);
                            return null;
                        }

                        String type = parts[0];
                        String itemId = parts[1];
                        int favorValue;
                        try {
                            favorValue = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid favor value in offerings for deity " + deityKey + ": " + parts[2]);
                            return null;
                        }

                        List<String> commands = new ArrayList<>();
                        if (parts.length > 3) {
                            commands = Arrays.asList(parts[3].split(";"));
                            plugin.debugLog("Loaded commands for offering: " + commands);
                        }

                        ItemStack itemStack;
                        if ("Saved".equalsIgnoreCase(type)) {
                            itemStack = loadSavedItem(itemId);
                            if (itemStack == null) {
                                plugin.getLogger().warning("Saved item not found: " + itemId + " for deity: " + deityKey);
                                return null;
                            }
                        } else {
                            Material material = Material.matchMaterial(itemId);
                            if (material == null) {
                                plugin.getLogger().warning("Invalid material in offerings for deity " + deityKey + ": " + itemId);
                                return null;
                            }
                            itemStack = new ItemStack(material);
                        }
                        return new Offering(itemStack, favorValue, commands);
                    })
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

            Deity deity = new Deity(plugin, name, lore, domain, alignment, favoredOfferings, favoredRituals, deityBlessings, deityCurses, deityMiracles);
            deityMap.put(deityKey.toLowerCase(), deity);
            plugin.getLogger().info("Loaded deity " + deity.getName() + " with " + favoredOfferings.size() + " offerings.");
        }

        return deityMap;
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