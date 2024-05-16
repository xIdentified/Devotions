package me.xidentified.devotions.storage;

import java.io.File;
import lombok.Getter;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.storage.model.IStorage;

@Getter
public class StorageManager {

    @Getter
    private File storageFolder;
    private final Devotions plugin;
    private IStorage storage;

    public StorageManager(Devotions plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        storageFolder = new File(plugin.getDataFolder(), "storage");
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }

        // Initialize the chosen storage type
        String storageType = plugin.getConfig().getString("storage.type", "yaml").toLowerCase();
        if ("mysql".equals(storageType)) {
            String host = plugin.getConfig().getString("storage.mysql.host");
            String port = plugin.getConfig().getString("storage.mysql.port");
            String database = plugin.getConfig().getString("storage.mysql.database");
            String username = plugin.getConfig().getString("storage.mysql.username");
            String password = plugin.getConfig().getString("storage.mysql.password");

            this.storage = new MySQLStorage(plugin, host, port, database, username, password);
        } else if ("sqlite".equals(storageType)) {
            this.storage = new SQLiteStorage(plugin);
        } else {
            this.storage = new YamlStorage(plugin, this);
        }
    }

}
