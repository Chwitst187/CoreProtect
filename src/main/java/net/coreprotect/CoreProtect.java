package net.coreprotect;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.coreprotect.config.ConfigHandler;
import net.coreprotect.consumer.Consumer;
import net.coreprotect.consumer.process.Process;
import net.coreprotect.language.Phrase;
import net.coreprotect.listener.player.PlayerQuitListener;
import net.coreprotect.paper.PaperAdapter;
import net.coreprotect.services.PluginInitializationService;
import net.coreprotect.utility.Chat;
import net.coreprotect.utility.Teleport;

/**
 * Main class for the CoreProtect plugin
 */
public final class CoreProtect extends JavaPlugin {

    private static final long ALERT_INTERVAL_MS = 30 * 1000; // 30 seconds
    private static final long MAX_SHUTDOWN_WAIT_MS = 15 * 60 * 1000; // 15 minutes
    private static final long DB_UNREACHABLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    private static CoreProtect instance;
    private boolean advancedChestsEnabled = false;

    /**
     * Get the instance of CoreProtect
     *
     * @return This CoreProtect instance
     */
    public static CoreProtect getInstance() {
        return instance;
    }

    private final CoreProtectAPI api = new CoreProtectAPI();

    /**
     * Get the CoreProtect API
     *
     * @return The CoreProtect API
     */
    public CoreProtectAPI getAPI() {
        return api;
    }

    @Override
    public void onEnable() {
        // Set plugin instance and data folder path
        instance = this;
        ConfigHandler.path = this.getDataFolder().getPath() + File.separator;

        advancedChestsEnabled = getServer().getPluginManager().getPlugin("AdvancedChests") != null;
        // Initialize plugin using the initialization service
        boolean initialized = PluginInitializationService.initializePlugin(this);

        // Disable plugin if initialization failed
        if (!initialized) {
            Chat.console(Phrase.build(Phrase.ENABLE_FAILED, ConfigHandler.EDITION_NAME));
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        safeShutdown(this);
    }

    private static void safeShutdown(Plugin plugin) {
        try {
            // Log disconnections of online players if server is stopping
            if (ConfigHandler.serverRunning && PaperAdapter.ADAPTER.isStopping(plugin.getServer())) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    PlayerQuitListener.queuePlayerQuit(player);
                }
            }

            // Revert any teleport blocks if not using Folia
            if (!ConfigHandler.isFolia) {
                revertTeleportBlocks();
            }

            ConfigHandler.serverRunning = false;
            long shutdownTime = System.currentTimeMillis();
            long nextAlertTime = shutdownTime + ALERT_INTERVAL_MS;

            if (ConfigHandler.converterRunning) {
                Chat.console(Phrase.build(Phrase.FINISHING_CONVERSION));
            }
            else {
                Chat.console(Phrase.build(Phrase.FINISHING_LOGGING));
            }

            if (ConfigHandler.migrationRunning) {
                ConfigHandler.purgeRunning = false;
            }

            waitForPendingOperations(shutdownTime, nextAlertTime);

            ConfigHandler.performDisable();
            Chat.console(Phrase.build(Phrase.DISABLE_SUCCESS, "CoreProtect v" + plugin.getDescription().getVersion()));
        }
        catch (Exception e) {
            e.printStackTrace();
            ConfigHandler.performDisable();
        }
    }

    private static void waitForPendingOperations(long shutdownTime, long nextAlertTime) throws InterruptedException {
        while ((Consumer.isRunning() || ConfigHandler.converterRunning) && !ConfigHandler.purgeRunning) {
            long currentTime = System.currentTimeMillis();

            if (currentTime >= nextAlertTime) {
                if (!ConfigHandler.converterRunning) {
                    int consumerId = (Consumer.currentConsumer == 1) ? 1 : 0;
                    int consumerCount = Consumer.getConsumerSize(consumerId) + Process.getCurrentConsumerSize();
                    Chat.console(Phrase.build(Phrase.LOGGING_ITEMS, String.format("%,d", consumerCount)));
                }
                nextAlertTime = currentTime + ALERT_INTERVAL_MS;
            }
            else if (!ConfigHandler.databaseReachable && (currentTime - shutdownTime) >= DB_UNREACHABLE_TIMEOUT_MS) {
                Chat.console(Phrase.build(Phrase.DATABASE_UNREACHABLE));
                break;
            }
            else if ((currentTime - shutdownTime) >= MAX_SHUTDOWN_WAIT_MS) {
                Chat.console(Phrase.build(Phrase.LOGGING_TIME_LIMIT));
                break;
            }

            Thread.sleep(100);
        }
    }

    private static void revertTeleportBlocks() {
        Iterator<Entry<Location, BlockData>> iterator = Teleport.revertBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Location, BlockData> entry = iterator.next();
            entry.getKey().getBlock().setBlockData(entry.getValue());
            iterator.remove();
        }
    }

    public boolean isAdvancedChestsEnabled() {
        return advancedChestsEnabled;
    }
}
