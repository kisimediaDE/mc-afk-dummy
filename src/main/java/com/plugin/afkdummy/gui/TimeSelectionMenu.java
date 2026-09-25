package com.plugin.afkdummy.gui;
import com.plugin.afkdummy.AFKDummyPlugin;
import com.plugin.afkdummy.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class TimeSelectionMenu extends MenuFramework {
    public TimeSelectionMenu(AFKDummyPlugin plugin, Player viewer) {
        super("AFK Dummy · Laufzeit", 27);
        int slot = 0;
        for (long duration : plugin.getConfigManager().getDurationOptions()) {
            setItem(slot++, createItem(Material.CLOCK, TimeUtil.formatDuration(duration), "Kostenlos", "Klicken zum Spawnen"), event -> {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();
                var session = plugin.getDummyManager().spawnDummy(player, player.getLocation(), duration);
                if (session != null) player.sendMessage("§aDummy gespawnt. Restzeit: " + session.getFormattedTimeRemaining());
            });
        }
        setItem(22, createItem(Material.PAPER, "Eigene Dauer", "/afkdummy spawn 1h30m"), event -> {
            event.getWhoClicked().closeInventory();
            event.getWhoClicked().sendMessage("§e/afkdummy spawn <Dauer>, z.B. 90m oder 1h30m");
        });
        setItem(26, createItem(Material.ARROW, "Zurück"), event -> new MainMenu(plugin, viewer).open(viewer));
        fillEmpty(Material.GRAY_STAINED_GLASS_PANE);
    }
}
