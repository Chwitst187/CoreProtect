package net.coreprotect.utility;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class StartupBanner {

    private StartupBanner() {
        throw new IllegalStateException("Utility class");
    }

    public static void show(JavaPlugin plugin) {
        String[] lines = {
                " ",
                "  _____ _          _     ",
                " / ____| |        (_)    ",
                "| |    | |__  _ __ _ ___ ",
                "| |    | '_ \\| '__| / __|",
                "| |____| | | | |  | \\__ \\",
                " \\_____|_| |_|_|  |_|___/",
                "  _____ _             _ _           ",
                " / ____| |           | (_)          ",
                "| (___ | |_ _   _  __| |_  ___  ___ ",
                " \\___ \\| __| | | |/ _` | |/ _ \\/ __|",
                " ____) | |_| |_| | (_| | | (_) \\__ \\",
                "|_____/ \\__|\\__,_|\\__,_|_|\\___/|___/"
        };

        io.papermc.paper.plugin.configuration.PluginMeta pluginMeta = plugin.getPluginMeta();
        List<String> authors = pluginMeta.getAuthors();
        String authorsStr = authors.isEmpty() ? "Unknown" : String.join(", ", authors);

        String divider = "§f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        String white = "§f";
        String purple = "§d";
        String orange = "§6";
        String green = "§a";
        String blue = "§9";
        String yellow = "§e";

        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        console.sendMessage(" ");
        console.sendMessage(divider);

        for (int i = 0; i < 7; i++) {
            console.sendMessage(purple + lines[i]);
        }

        for (int i = 7; i < lines.length; i++) {
            console.sendMessage(orange + lines[i]);
        }

        console.sendMessage(" ");
        console.sendMessage(white + "Plugin: " + green + pluginMeta.getName() + " v" + pluginMeta.getVersion());
        console.sendMessage(white + "Author: " + blue + authorsStr);
        console.sendMessage(white + "Studio: " + yellow + "CoreProtect");
        console.sendMessage(white + "Server: " + yellow + Bukkit.getVersion());
        console.sendMessage(" ");
        console.sendMessage(divider);
    }
}
