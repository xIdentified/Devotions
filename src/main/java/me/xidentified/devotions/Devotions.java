package me.xidentified.devotions;

import me.xidentified.devotions.commandexecutors.*;
import me.xidentified.devotions.listeners.PlayerListener;
import me.xidentified.devotions.listeners.RitualListener;
import me.xidentified.devotions.listeners.ShrineListener;
import me.xidentified.devotions.managers.*;
import me.xidentified.devotions.rituals.*;
import me.xidentified.devotions.storage.DevotionStorage;
import me.xidentified.devotions.storage.ShrineStorage;
import me.xidentified.devotions.storage.StorageManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Devotions extends JavaPlugin {
    private static Devotions instance;
    private DevotionManager devotionManager;
    private RitualManager ritualManager;
    private final Map<String, Miracle> miraclesMap = new HashMap<>();
    private CooldownManager cooldownManager;
    private MeditationManager meditationManager;
    private ShrineListener shrineListener;
    private YamlConfiguration deitiesConfig;
    private FileConfiguration ritualConfig;
    private FileConfiguration soundsConfig;
    private StorageManager storageManager;
    private DevotionStorage devotionStorage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializePlugin();
        loadSoundsConfig();
    }

    private Map<String, Deity> loadDeities(YamlConfiguration deitiesConfig) {
        Map<String, Deity> deityMap = new HashMap<>();
        ConfigurationSection deitiesSection = deitiesConfig.getConfigurationSection("deities");
        for (String deityKey : deitiesSection.getKeys(false)) {
            ConfigurationSection deityConfig = deitiesSection.getConfigurationSection(deityKey);

            String name = deityConfig.getString("name");
            String lore = deityConfig.getString("lore");
            String domain = deityConfig.getString("domain");
            String alignment = deityConfig.getString("alignment");
            List<String> favoredRituals = deityConfig.getStringList("rituals");

            // Load offerings
            List<String> offeringStrings = deityConfig.getStringList("offerings");
            List<ItemStack> favoredOfferings = offeringStrings.stream()
                    .map(offering -> {
                        String[] parts = offering.split(":");
                        Material material = Material.matchMaterial(parts[0]);
                        if (material == null) {
                            getLogger().warning("Invalid material in offerings for deity " + deityKey + ": " + parts[0]);
                            return null;
                        }
                        int favor;
                        try {
                            favor = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            getLogger().warning("Invalid favor value in offerings for deity " + deityKey + ": " + parts[1]);
                            return null;
                        }
                        return new ItemStack(material, favor);
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
                    debugLog("Failed to parse miracle: " + miracleString + " for deity " + deityKey);
                }
            }

            Deity deity = new Deity(this, name, lore, domain, alignment, favoredOfferings, favoredRituals, deityBlessings, deityCurses, deityMiracles);
            deityMap.put(deityKey.toLowerCase(), deity);
            getLogger().info("Loaded deity " + deity.getName() + " with offerings: " + favoredOfferings.size());
        }

        return deityMap;
    }

    private Miracle parseMiracle(String miracleString) {
        debugLog("Parsing miracle: " + miracleString);
        MiracleEffect effect;
        List<Condition> conditions = new ArrayList<>();

        String[] parts = miracleString.split(":", 2);
        String miracleType = parts[0];  // Define miracleType here

        debugLog("Parsed miracleString: " + miracleString);
        debugLog("Miracle type: " + miracleType);
        if (parts.length > 1) {
            debugLog("Command/Argument: " + parts[1]);
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
                        effect = new DoubleCropDropsEffect(this, duration);
                        conditions.add(new NearCropsCondition());
                    } catch (NumberFormatException e) {
                        debugLog("Invalid duration provided for double_crops miracle.");
                        return null;
                    }
                } else {
                    debugLog("No duration provided for double_crops miracle.");
                    return null;
                }
            }
            case "run_command" -> {
                if (parts.length > 1) {
                    String command = parts[1];
                    effect = new ExecuteCommandEffect(command);
                } else {
                    debugLog("No command provided for run_command miracle.");
                    return null;
                }
            }
            default -> {
                debugLog("Unrecognized miracle encountered in parseMiracle!");
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

    public YamlConfiguration getDeitiesConfig() {
        if (deitiesConfig == null) {
            File deitiesFile = new File(getDataFolder(), "deities.yml");
            deitiesConfig = YamlConfiguration.loadConfiguration(deitiesFile);
        }
        return deitiesConfig;
    }

    private void loadRituals() {
        ConfigurationSection ritualsSection = ritualConfig.getConfigurationSection("rituals");
        if (ritualsSection == null) {
            getLogger().warning("No rituals section found in config.");
            return;
        }

        // Test to print out the structure of the rituals section
        debugLog("Rituals Section Keys: " + ritualsSection.getKeys(true));

        for (String key : ritualsSection.getKeys(false)) {
            try {
                String path = "rituals." + key + ".";

                // Parse general info
                String displayName = ritualConfig.getString(path + "display_name");
                String description = ritualConfig.getString(path + "description");
                int favorReward = ritualConfig.getInt(path + "favor");

                // Parse item
                String itemType = ritualConfig.getString(path + "item.type");
                String itemId = ritualConfig.getString(path + "item.id");
                RitualItem ritualItem = new RitualItem(itemType, itemId);

                // Parse conditions
                String time = ritualConfig.getString(path + "conditions.time");
                String biome = ritualConfig.getString(path + "conditions.biome");
                String weather = ritualConfig.getString(path + "conditions.weather");
                String moonPhase = ritualConfig.getString(path + "conditions.moon_phase");
                double minAltitude = ritualConfig.getDouble(path + "conditions.min_altitude", 0.0); // Default to 0 if not found
                int minExperience = ritualConfig.getInt(path + "conditions.min_experience", 0); // Default to 0 if not found
                double minHealth = ritualConfig.getDouble(path + "conditions.min_health", 0.0); // Default to 0 if not found
                int minHunger = ritualConfig.getInt(path + "conditions.min_hunger", 0); // Default to 0 if not found
                RitualConditions ritualConditions = new RitualConditions(time, biome, weather, moonPhase,
                        minAltitude, minExperience, minHealth, minHunger);

                // Parse outcome
                List<String> outcomeCommands;
                if (ritualConfig.isList(path + "outcome-command")) {
                    outcomeCommands = ritualConfig.getStringList(path + "outcome-command");
                } else {
                    String singleCommand = ritualConfig.getString(path + "outcome-command");
                    if (singleCommand == null || singleCommand.isEmpty()) {
                        getLogger().warning("No outcome specified for ritual: " + key);
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

                        RitualObjective objective = new RitualObjective(this, type, objDescription, target, count);
                        objectives.add(objective);
                        debugLog("Loaded objective " + objDescription + " for ritual " + key);
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to load objectives for ritual: " + key);
                    e.printStackTrace();
                }


                // Create and store the ritual
                Ritual ritual = new Ritual(this, displayName, description, ritualItem, favorReward, ritualConditions, ritualOutcome, objectives);
                RitualManager.getInstance(this).addRitual(key, ritual);  // Store the ritual with its key
                getLogger().info("Loaded ritual " + displayName + " with key: " + key + ", favor amount " + favorReward + ", and objectives " + objectives);
            } catch (Exception e) {
                getLogger().severe("Failed to load ritual with key: " + key);
                e.printStackTrace();
            }
        }
    }

    public void spawnParticles(Location location, Particle particle, int count, double radius, double velocity) {
        World world = location.getWorld();
        Particle.DustOptions dustOptions = null;
        if (particle == Particle.REDSTONE) {
            dustOptions = new Particle.DustOptions(Color.RED, 1.0f);  // You can adjust the color and size as needed
        }

        for (int i = 0; i < count; i++) {
            double phi = Math.acos(2 * Math.random() - 1);  // Angle for elevation
            double theta = 2 * Math.PI * Math.random();     // Angle for azimuth

            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);

            Location particleLocation = location.clone().add(x, y, z);
            Vector direction = particleLocation.toVector().subtract(location.toVector()).normalize().multiply(velocity);

            if (dustOptions != null) {
                world.spawnParticle(particle, particleLocation, 0, direction.getX(), direction.getY(), direction.getZ(), 0, dustOptions);
            } else {
                world.spawnParticle(particle, particleLocation, 0, direction.getX(), direction.getY(), direction.getZ(), 0);
            }
        }
    }

    public void spawnRitualMobs(Location center, EntityType entityType, int count, double radius) {
        World world = center.getWorld();
        List<Location> validLocations = new ArrayList<>();

        // Find valid spawn locations
        for (int i = 0; i < count * 10; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = center.getX() + radius * Math.sin(angle);
            double z = center.getZ() + radius * Math.cos(angle);
            Location potentialLocation = new Location(world, x, center.getY(), z);
            Block block = potentialLocation.getBlock();
            if (block.getType() == Material.AIR && block.getRelative(BlockFace.DOWN).getType().isSolid() && block.getRelative(BlockFace.UP).getType() == Material.AIR) {
                validLocations.add(potentialLocation);
            }
        }

        // Spawn mobs at valid locations
        for (int i = 0; i < Math.min(count, validLocations.size()); i++) {
            world.spawnEntity(validLocations.get(i), entityType);
        }
    }

    public void reloadConfigurations() {
        reloadConfig();
        reloadRitualConfig();
        reloadDeitiesConfig();
        reloadSoundsConfig();
        initializePlugin();
    }

    private void loadRitualConfig() {
        File ritualFile = new File(getDataFolder(), "rituals.yml");
        if (!ritualFile.exists()) {
            saveResource("rituals.yml", false);
        }
        ritualConfig = YamlConfiguration.loadConfiguration(ritualFile);
    }

    private void reloadRitualConfig() {
        File ritualFile = new File(getDataFolder(), "rituals.yml");
        if (ritualFile.exists()) {
            ritualConfig = YamlConfiguration.loadConfiguration(ritualFile);
        }
        loadRituals();
    }

    private Map<String, Deity> reloadDeitiesConfig() {
        File deitiesFile = new File(getDataFolder(), "deities.yml");
        if (!deitiesFile.exists()) {
            // The file does not exist, let's extract it from the JAR
            saveResource("deities.yml", false);
        }

        if (deitiesFile.exists()) {
            deitiesConfig = YamlConfiguration.loadConfiguration(deitiesFile);
            return loadDeities(deitiesConfig);
        } else {
            getLogger().severe("Unable to create default deities.yml");
            return new HashMap<>(); // Return an empty map as a fallback
        }
    }

    private void reloadSoundsConfig() {
        File soundFile = new File(getDataFolder(), "sounds.yml");
        if (soundFile.exists()) {
            soundsConfig = YamlConfiguration.loadConfiguration(soundFile);
        }
        loadRituals();
    }

    public void loadSoundsConfig() {
        File soundsFile = new File(getDataFolder(), "sounds.yml");
        if (!soundsFile.exists()) {
            saveResource("sounds.yml", false);
        }
        soundsConfig = YamlConfiguration.loadConfiguration(soundsFile);
        debugLog("sounds.yml successfully loaded!");
    }

    private void initializePlugin() {
        // Load configurations
        instance = this;
        loadRitualConfig();

        // Initiate manager classes
        this.storageManager = new StorageManager(this);
        Map<String, Deity> loadedDeities = reloadDeitiesConfig();
        this.devotionStorage = new DevotionStorage(storageManager);
        this.devotionManager = new DevotionManager(this, loadedDeities);
        ShrineManager shrineManager = new ShrineManager(this);
        loadRituals();
        ShrineStorage shrineStorage = new ShrineStorage(storageManager);
        shrineManager.setShrineStorage(shrineStorage);
        ritualManager = RitualManager.getInstance(this);
        this.cooldownManager = new CooldownManager(this);
        this.meditationManager = new MeditationManager(this);
        FavorCommand favorCmd = new FavorCommand(this);
        ShrineCommandExecutor shrineCmd = new ShrineCommandExecutor(devotionManager, shrineManager);
        DeityCommand deityCmd = new DeityCommand(this);
        RitualCommand ritualCommand = new RitualCommand(this);

        // Register commands
        Objects.requireNonNull(getCommand("deity")).setExecutor(deityCmd);
        Objects.requireNonNull(getCommand("deity")).setTabCompleter(deityCmd);
        Objects.requireNonNull(getCommand("favor")).setExecutor(favorCmd);
        Objects.requireNonNull(getCommand("favor")).setTabCompleter(favorCmd);
        Objects.requireNonNull(getCommand("shrine")).setExecutor(shrineCmd);
        Objects.requireNonNull(getCommand("shrine")).setTabCompleter(shrineCmd);
        Objects.requireNonNull(getCommand("devotions")).setExecutor(new DevotionsCommandExecutor(this));
        Objects.requireNonNull(getCommand("ritual")).setExecutor(ritualCommand);
        Objects.requireNonNull(getCommand("ritual")).setTabCompleter(ritualCommand);

        // Register admin commands
        TestMiracleCommand testMiracleCmd = new TestMiracleCommand(miraclesMap);
        Objects.requireNonNull(getCommand("testmiracle")).setExecutor(testMiracleCmd);

        // Register listeners
        this.shrineListener = new ShrineListener(this, shrineManager, cooldownManager);
        RitualListener.initialize(this, shrineManager);
        getServer().getPluginManager().registerEvents(new DoubleCropDropsEffect(this, 90), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Register events
        getServer().getPluginManager().registerEvents(RitualListener.getInstance(), this);
        getServer().getPluginManager().registerEvents(shrineListener, this);
        getServer().getPluginManager().registerEvents(shrineCmd, this);

        debugLog("Devotions successfully initialized!");
    }

    @Override
    public void onDisable() {
        // Save all player devotions to ensure data is not lost on shutdown
        devotionManager.saveAllPlayerDevotions();

        // Cancel tasks
        getServer().getScheduler().cancelTasks(this);

        ritualManager.ritualDroppedItems.clear();

        debugLog("Devotions has been disabled and all data saved!");
    }




    public static Devotions getInstance() {
        return instance;
    }

    public DevotionManager getDevotionManager() {return this.devotionManager;}

    public RitualManager getRitualManager() {return ritualManager;}

    public CooldownManager getCooldownManager() {return cooldownManager;}

    public MeditationManager getMeditationManager() {return meditationManager;}

    public ShrineListener getShrineListener() {return shrineListener;}

    public DevotionStorage getDevotionStorage() {return devotionStorage;}

    public StorageManager getStorageManager() {return storageManager;}

    public int getShrineLimit() {
        return getConfig().getInt("shrine-limit", 3); // 3 is a default value if not set in config
    }

    public void debugLog(String message) {
        if (getConfig().getBoolean("debug_mode")) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public void playConfiguredSound(Player player, String key) {
        if (soundsConfig == null) {
            debugLog("soundsConfig is null.");
            return;
        }

        String soundKey = "sounds." + key;
        if (soundsConfig.contains(soundKey)) {
            String soundName = soundsConfig.getString(soundKey + ".sound");
            float volume = (float) soundsConfig.getDouble(soundKey + ".volume");
            float pitch = (float) soundsConfig.getDouble(soundKey + ".pitch");

            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } else {
            debugLog("Sound " + soundKey + " not found in sounds.yml!");
        }
    }

}
