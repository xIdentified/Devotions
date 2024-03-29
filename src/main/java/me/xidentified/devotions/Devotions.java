package me.xidentified.devotions;

import de.cubbossa.tinytranslations.Message;
import de.cubbossa.tinytranslations.TinyTranslationsBukkit;
import de.cubbossa.tinytranslations.Translator;
import de.cubbossa.tinytranslations.TinyTranslations;
import de.cubbossa.tinytranslations.persistent.YamlMessageStorage;
import de.cubbossa.tinytranslations.persistent.YamlStyleStorage;
import lombok.Getter;
import me.xidentified.devotions.commandexecutors.*;
import me.xidentified.devotions.listeners.PlayerListener;
import me.xidentified.devotions.listeners.RitualListener;
import me.xidentified.devotions.listeners.ShrineListener;
import me.xidentified.devotions.managers.*;
import me.xidentified.devotions.storage.StorageManager;
import me.xidentified.devotions.util.Messages;
import me.xidentified.devotions.util.Metrics;
import me.xidentified.devotions.util.Placeholders;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

@Getter
public class Devotions extends JavaPlugin {
    @Getter private static Devotions instance;
    private final DevotionsConfig devotionsConfig = new DevotionsConfig(this);
    private DevotionManager devotionManager;
    private RitualManager ritualManager;
    private CooldownManager cooldownManager;
    private MeditationManager meditationManager;
    private ShrineListener shrineListener;
    private StorageManager storageManager;
    private Translator translations;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializePlugin();
        devotionsConfig.loadSoundsConfig();
        devotionsConfig.reloadSavedItemsConfig();

        TinyTranslationsBukkit.enable(this);
        translations = TinyTranslationsBukkit.application(this);
        translations.setMessageStorage(new YamlMessageStorage(new File(getDataFolder(), "/lang/")));
        translations.setStyleStorage(new YamlStyleStorage(new File(getDataFolder(), "/lang/styles.yml")));
        translations.addMessages(TinyTranslations.messageFieldsFromClass(Messages.class));

        loadLanguages();

        // Set the LocaleProvider
        translations.setLocaleProvider(audience -> {
            // Read settings from config
            boolean usePlayerClientLocale = getConfig().getBoolean("use-player-client-locale", true);
            String fallbackLocaleCode = getConfig().getString("default-locale", "en");
            Locale fallbackLocale = Locale.forLanguageTag(fallbackLocaleCode);

            if (audience == null || !usePlayerClientLocale) {
                return fallbackLocale;
            }

            return audience.getOrDefault(Identity.LOCALE, fallbackLocale);
        });

        // If PAPI is installed we'll register placeholders
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders(this).register();
            debugLog("PlaceholderAPI expansion enabled!");
        }

        // Register bStats
        int pluginId = 20922;
        Metrics metrics = new Metrics(this, pluginId);
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

    /**
     * Run to reload changes to message files
     */
    public void loadLanguages() {

        if (!new File(getDataFolder(), "/lang/styles.yml").exists()) {
            saveResource("lang/styles.yml", false);
        }
        translations.loadStyles();

        // save default translations
        translations.saveLocale(Locale.ENGLISH);
        if (!new File(getDataFolder(), "/lang/de.yml").exists()) {
            this.saveResource("lang/de.yml", false);
        }

        // load
        translations.loadLocales();
    }

    public void initializePlugin() {
        HandlerList.unregisterAll(this);
        instance = this;
        devotionsConfig.loadRitualConfig();

        // Initialize storage
        this.storageManager = new StorageManager(this);

        // Clear existing data before re-initializing
        if (devotionManager != null) {
            devotionManager.clearData();
        }

        Map<String, Deity> loadedDeities = devotionsConfig.reloadDeitiesConfig();
        this.devotionManager = new DevotionManager(this, loadedDeities);
        ShrineManager shrineManager = new ShrineManager(this);

        // Load all shrines using the ShrineManager
        shrineManager.setStorage(storageManager.getStorage());
        devotionsConfig.loadRituals();

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
        TestMiracleCommand testMiracleCmd = new TestMiracleCommand(devotionsConfig.getMiraclesMap());
        Objects.requireNonNull(getCommand("testmiracle")).setExecutor(testMiracleCmd);

        // Register listeners
        this.shrineListener = new ShrineListener(this, shrineManager, cooldownManager);
        RitualListener.initialize(this, shrineManager);
        getServer().getPluginManager().registerEvents(new DoubleCropDropsEffect(this, 90), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, storageManager), this);

        // Register events
        getServer().getPluginManager().registerEvents(RitualListener.getInstance(), this);
        getServer().getPluginManager().registerEvents(shrineListener, this);
        getServer().getPluginManager().registerEvents(shrineCmd, this);

        debugLog("Devotions successfully initialized!");
    }

    @Override
    public void onDisable() {
        // Unregister all listeners
        HandlerList.unregisterAll(this);

        // Save all player devotions to ensure data is not lost on shutdown
        devotionManager.saveAllPlayerDevotions();

        // Close the database connection
        storageManager.getStorage().closeConnection();

        // Cancel tasks
        getServer().getScheduler().cancelTasks(this);
        ritualManager.ritualDroppedItems.clear();

        translations.close();
    }

    public void debugLog(String message) {
        if (getConfig().getBoolean("debug_mode")) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public void playConfiguredSound(Player player, String key) {
        if (devotionsConfig.getSoundsConfig() == null) {
            debugLog("soundsConfig is null.");
            return;
        }

        String soundKey = "sounds." + key;
        FileConfiguration soundsConfig = devotionsConfig.getSoundsConfig();

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

    public void sendMessage(CommandSender sender, ComponentLike componentLike) {
        if (componentLike instanceof Message msg) {
            // Translate the message into the locale of the command sender
            componentLike = translations.process(msg, TinyTranslationsBukkit.getLocale(sender));
        }
        TinyTranslationsBukkit.sendMessage(sender, componentLike);
    }

}
