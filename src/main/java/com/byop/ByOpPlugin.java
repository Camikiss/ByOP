package com.byop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class ByOpPlugin extends JavaPlugin implements Listener {

    private static final Map<String, String> DEFAULT_MESSAGES = new LinkedHashMap<String, String>();
    private static final Map<String, Object> DEFAULT_SETTINGS = new LinkedHashMap<String, Object>();

    static {
        DEFAULT_MESSAGES.put("prefix", "&4[ByOp] &c");
        DEFAULT_MESSAGES.put("no-permission", "&cNo tienes permiso para usar este comando.");
        DEFAULT_MESSAGES.put("usage", "&cUso: /byop reload | list");
        DEFAULT_MESSAGES.put("op-blocked-player", "&cNo puedes dar OP a &f%target%&c. Ese nick no esta autorizado.");
        DEFAULT_MESSAGES.put("op-blocked-console", "&cComando /op bloqueado: &f%target%&c no esta autorizado.");
        DEFAULT_MESSAGES.put("op-removed", "&cTu OP fue retirado: no estas autorizado.");
        DEFAULT_MESSAGES.put("reloaded", "&aLista recargada. Nicks permitidos: &f%count%");
        DEFAULT_MESSAGES.put("list", "&eNicks permitidos (&f%count%&e): &f%names%");
        DEFAULT_MESSAGES.put("empty-names", "&7ninguno");

        DEFAULT_SETTINGS.put("deop-unauthorized", Boolean.TRUE);
        DEFAULT_SETTINGS.put("check-interval-ticks", 40);
    }

    private volatile Set<String> allowed = Collections.emptySet();
    private volatile Map<String, String> displayNames = Collections.emptyMap();
    private volatile boolean listLoaded = false;

    private File allowedFile;
    private BukkitTask checkTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureDefaults();

        if (getDataFolder() != null && !getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        allowedFile = new File(getDataFolder(), "allowed.txt");
        if (!allowedFile.exists()) {
            saveResource("allowed.txt", false);
        }

        loadAllowed();

        getServer().getPluginManager().registerEvents(this, this);

        if (classExists("org.bukkit.event.server.ServerCommandEvent")) {
            getServer().getPluginManager().registerEvents(new ServerCommandListener(this), this);
        } else {
            getLogger().warning("Este servidor no soporta el bloqueo de /op por consola (muy antiguo).");
        }

        if (getConfig().getBoolean("deop-unauthorized", true)) {
            startCheckTask();
        }

        getLogger().info("ByOp by CamiKiss activado. Nicks permitidos: " + allowed.size());
    }

    @Override
    public void onDisable() {
        stopCheckTask();
        allowed = Collections.emptySet();
        displayNames = Collections.emptyMap();
        listLoaded = false;
    }

    private boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void ensureDefaults() {
        boolean changed = false;
        Set<String> existing = getConfig().getKeys(true);
        for (Map.Entry<String, String> entry : DEFAULT_MESSAGES.entrySet()) {
            String path = "messages." + entry.getKey();
            if (!existing.contains(path)) {
                getConfig().set(path, entry.getValue());
                changed = true;
            }
        }
        for (Map.Entry<String, Object> entry : DEFAULT_SETTINGS.entrySet()) {
            if (!existing.contains(entry.getKey())) {
                getConfig().set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (changed) {
            saveConfig();
        }
    }

    public void loadAllowed() {
        List<String> lines = readAllowedLines();
        if (lines == null) {
            return;
        }

        Set<String> newAllowed = new LinkedHashSet<String>();
        Map<String, String> newDisplay = new LinkedHashMap<String, String>();
        for (String line : lines) {
            String name = lineName(line);
            if (name != null) {
                addEntry(newAllowed, newDisplay, name);
            }
        }

        allowed = newAllowed;
        displayNames = newDisplay;
        listLoaded = true;
    }

    public boolean isAllowed(String name) {
        return name != null && allowed.contains(name.toLowerCase(Locale.ROOT));
    }

    public void enforceOps() {
        if (!listLoaded) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                if (player.isOp() && !isAllowed(player.getName())) {
                    player.setOp(false);
                    player.sendMessage(msg("op-removed", "%player%", player.getName()));
                    getLogger().warning("OP retirado a " + player.getName() + " (no autorizado)");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        if (!getConfig().getBoolean("deop-unauthorized", true)) {
            return;
        }
        getServer().getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
                if (!listLoaded) {
                    return;
                }
                Player player = event.getPlayer();
                if (player.isOnline() && player.isOp() && !isAllowed(player.getName())) {
                    player.setOp(false);
                    player.sendMessage(msg("op-removed", "%player%", player.getName()));
                    getLogger().warning("OP retirado a " + player.getName() + " (no autorizado)");
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String target = opTarget(event.getMessage());
        if (target == null || isAllowed(target)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(msg("op-blocked-player",
                "%target%", target, "%player%", event.getPlayer().getName()));
        getLogger().warning("Jugador " + event.getPlayer().getName() + " intento dar OP a " + target
                + " (bloqueado)");
    }

    public static String opTarget(String raw) {
        if (raw == null) {
            return null;
        }
        String command = raw.trim();
        while (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.isEmpty()) {
            return null;
        }

        String[] parts = command.split("\\s+");
        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.lastIndexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        if (!label.equals("op") || parts.length < 2) {
            return null;
        }
        return parts[1];
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("byop")) {
            return false;
        }
        if (!sender.hasPermission("byop.admin")) {
            sender.sendMessage(msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(msg("usage"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            reloadConfig();
            ensureDefaults();
            loadAllowed();
            stopCheckTask();
            if (getConfig().getBoolean("deop-unauthorized", true)) {
                startCheckTask();
            }
            sender.sendMessage(msg("reloaded", "%count%", String.valueOf(allowed.size())));
            return true;
        }

        if (sub.equals("list")) {
            if (!listLoaded) {
                loadAllowed();
            }
            StringBuilder builder = new StringBuilder();
            for (String name : displayNames.values()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(name);
            }
            String names = builder.length() == 0 ? text("empty-names") : builder.toString();
            sender.sendMessage(msg("list", "%count%", String.valueOf(allowed.size()),
                    "%names%", names));
            return true;
        }

        sender.sendMessage(msg("usage"));
        return true;
    }

    private String lineName(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        String[] parts = trimmed.split("\\s+");
        return parts.length == 0 ? null : parts[0];
    }

    private void addEntry(Set<String> target, Map<String, String> display, String name) {
        String clean = name.trim();
        if (clean.isEmpty()) {
            return;
        }
        String lower = clean.toLowerCase(Locale.ROOT);
        target.add(lower);
        if (!display.containsKey(lower)) {
            display.put(lower, clean);
        }
    }

    private List<String> readAllowedLines() {
        List<String> lines = new ArrayList<String>();
        if (allowedFile == null || !allowedFile.exists()) {
            return lines;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(allowedFile), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (Exception e) {
            getLogger().warning("No se pudo leer allowed.txt: " + e.getMessage());
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void startCheckTask() {
        if (checkTask != null) {
            return;
        }
        long interval = Math.max(20L, getConfig().getLong("check-interval-ticks", 40L));
        checkTask = getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                enforceOps();
            }
        }, interval, interval);
    }

    private void stopCheckTask() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    public String msg(String key, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", DEFAULT_MESSAGES.get("prefix"));
        String text = getConfig().getString("messages." + key, DEFAULT_MESSAGES.get(key));
        if (prefix == null) {
            prefix = "";
        }
        if (text == null) {
            text = "";
        }
        String full = prefix + text;
        if (replacements != null) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                full = full.replace(replacements[i], replacements[i + 1]);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', full);
    }

    public String text(String key) {
        String text = getConfig().getString("messages." + key, DEFAULT_MESSAGES.get(key));
        if (text == null) {
            text = "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
