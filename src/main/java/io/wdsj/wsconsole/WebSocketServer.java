package io.wdsj.wsconsole;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.wdsj.wsconsole.WSConsole.config;
import static io.wdsj.wsconsole.WSConsole.getScheduler;

public class WebSocketServer extends org.java_websocket.server.WebSocketServer {
    private final WSConsole plugin = WSConsole.getInstance();

    public WebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        plugin.getLogger().info(conn.getRemoteSocketAddress().getAddress().getHostAddress()+ " connected.");
        String connectionURL = handshake.getResourceDescriptor();
        Map<String, String> queryParams = parseQueryString(connectionURL);
        String username = queryParams.get("username");
        String password = queryParams.get("password");

        if (authenticate(username, password)) {
            conn.send("Welcome to the server, "+username+"!");
            plugin.getLogger().info(conn.getRemoteSocketAddress().getAddress().getHostAddress()+ " authenticated as " + username + ".");
        } else {
            conn.send("Authentication failed.");
            plugin.getLogger().info(conn.getRemoteSocketAddress().getAddress().getHostAddress()+ " authentication failed.");
            conn.close();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        plugin.getLogger().info(conn.getRemoteSocketAddress().getAddress().getHostAddress()+ " disconnected.");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("/")) {
            getScheduler().runTask(()->{
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', message.substring(1)));
            });
            conn.send("Executed command: "+message.substring(1));
        } else {
            getScheduler().runTask(()-> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),ChatColor.translateAlternateColorCodes('&', "say "+message));
            });
            conn.send("Sended message: "+message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // 发生错误时的逻辑
    }

    @Override
    public void onStart() {
    }
    private Map<String, String> parseQueryString(String rawUrl) {
        Map<String, String> queryParams = new HashMap<>();

        String[] urlParts = rawUrl.split("\\?");
        if (urlParts.length > 1) {
            String queryString = urlParts[1];
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }

        return queryParams;
    }
    private boolean authenticate(String username, String password) {
        List<String> accounts = config.getStringList("accounts");
        for (String account : accounts) {
            String[] splited = account.split("\\|");
            if (splited.length == 2 && splited[0].equals(username) && splited[1].equals(password)) {
                return true;
            }
        }
        return false;
    }
}
