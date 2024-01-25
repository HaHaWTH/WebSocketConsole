package io.wdsj.wsconsole;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public final class WSConsole extends JavaPlugin {
    public static YamlConfiguration config;
    public static WSConsole getInstance(){
        return instance;
    }
    public static TaskScheduler getScheduler(){
        return scheduler;
    }
    private static WSConsole instance;
    private static TaskScheduler scheduler;
    public WebSocketServer server = null;
    Handler handler;


    @Override
    public void onEnable() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
        loadConfig();
        if (config.getBoolean("startOnEnabled")) {
            CompletableFuture.runAsync(this::startWebsocketServer);
            getLogger().info("WebSocket server started on "+config.getString("bindIp")+":"+config.getInt("bindPort"));
        }
        Objects.requireNonNull(getCommand("wsconsole")).setExecutor(this);
        handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                String message = record.getMessage();
                if (server != null && !server.getConnections().isEmpty()) {
                    server.broadcast(message);
                }
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        };
        Bukkit.getLogger().addHandler(handler);
        Plugin[] plugins = getServer().getPluginManager().getPlugins();
        for (Plugin plugin : plugins) {
            if (!plugin.equals(this)) {
                plugin.getLogger().addHandler(handler);
            }
        }
        getLogger().warning("[WSCONSOLE Warning] Preview version, does not stand for the FINAL quality.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0) {
            if (args[0].equalsIgnoreCase("start")) {
                if (server != null) {
                    sender.sendMessage("WebSocket server is already running");
                    return true;
                }
                loadConfig();
                startWebsocketServer();
                sender.sendMessage("WebSocket server started on "+config.getString("bindIp")+":"+config.getInt("bindPort"));
            } else if (args[0].equalsIgnoreCase("stop")) {
                try {
                    server.stop();
                    server = null;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                sender.sendMessage("WebSocket server stopped");
            } else if (args[0].equalsIgnoreCase("send")) {
                if (server != null && args.length > 1) {
                    server.broadcast(args[1]);
                    sender.sendMessage("Broadcast message " + args[1] + " to all clients");
                } else {
                    sender.sendMessage("WebSocket server is not running yet");
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                loadConfig();
                sender.sendMessage("Reloaded config");
            } else {
                sender.sendMessage("Usage: /wsconsole [start|stop|send|reload]");
            }

        } else {
            sender.sendMessage("Usage: /wsconsole [start|stop|send|reload]");
        }
        return true;
    }



    @Override
    public void onDisable() {
        if (server != null) {
            try {
                server.stop();
                server = null;
                getLogger().info("WebSocket server stopped");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Objects.requireNonNull(getCommand("wsconsole")).setExecutor(null);
        Bukkit.getLogger().removeHandler(handler);
        Plugin[] plugins = getServer().getPluginManager().getPlugins();
        for (Plugin plugin : plugins) {
            plugin.getLogger().removeHandler(handler);
        }

    }
    private void loadConfig() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }
    private void startWebsocketServer() {
        InetSocketAddress address = new InetSocketAddress(Objects.requireNonNull(config.getString("bindIp")), config.getInt("bindPort"));
        server = new WebSocketServer(address);
        server.start();
    }

}
