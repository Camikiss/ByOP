package com.byop;

import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

public class ServerCommandListener implements Listener {

    private final ByOpPlugin plugin;

    public ServerCommandListener(ByOpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        String target = ByOpPlugin.opTarget(event.getCommand());
        if (target == null || plugin.isAllowed(target)) {
            return;
        }

        event.getSender().sendMessage(plugin.msg("op-blocked-console", "%target%", target));
        plugin.getLogger().warning("Comando /op bloqueado (consola/RCON) para: " + target);

        if (event instanceof Cancellable) {
            ((Cancellable) event).setCancelled(true);
        } else {
            event.setCommand("byop:op-bloqueado");
        }
    }
}
