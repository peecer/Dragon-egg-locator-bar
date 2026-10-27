package com.peecer.dragonegglocator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Gives exactly one online dragon-egg holder access to the vanilla locator bar.
 * All other players continue transmitting their waypoints, so the holder can
 * actually see them; only receiving is restricted.
 */
public final class DragonEggLocatorPlugin extends JavaPlugin implements Listener {

    private static final double ENABLED_RECEIVE_RANGE = 60_000_000.0;
    private static final double DISABLED_RECEIVE_RANGE = 0.0;

    // Remember original attribute settings so disabling the plugin (or logging
    // out) does not leave players with a permanently disabled locator bar.
    private final Map<UUID, Double> originalRanges = new HashMap<>();
    private UUID currentHolder;
    private int ticks;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        enableLocatorGameRules();
        refreshHolders();

        // Reconcile every tick: catches placing, dropping, death, pickups,
        // creative inventory edits, commands, container transfers, and joins.
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (++ticks % 20 == 0) {
                enableLocatorGameRules();
            }
            refreshHolders();
        }, 1L, 1L);
        getLogger().info("Dragon Egg Locator is active.");
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            restorePlayer(player);
        }
        originalRanges.clear();
        currentHolder = null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Execute after the player's inventory has been fully attached.
        getServer().getScheduler().runTask(this, this::refreshHolders);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        restorePlayer(event.getPlayer());
        if (event.getPlayer().getUniqueId().equals(currentHolder)) {
            currentHolder = null;
        }
        // The normal tick handles selecting the next player with the egg.
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        enableLocatorGameRule(event.getWorld());
    }

    private void refreshHolders() {
        Player holder = currentHolder == null ? null : getServer().getPlayer(currentHolder);
        if (holder == null || !holder.isOnline() || !hasEgg(holder)) {
            holder = null;
            for (Player player : getServer().getOnlinePlayers()) {
                if (hasEgg(player)) {
                    holder = player;
                    break;
                }
            }
        }
        // Preserve the current holder if multiple eggs are created through
        // commands. Never grant access to more than one player simultaneously.
        currentHolder = holder == null ? null : holder.getUniqueId();

        for (Player player : getServer().getOnlinePlayers()) {
            AttributeInstance range = player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE);
            if (range == null) {
                continue;
            }
            originalRanges.putIfAbsent(player.getUniqueId(), range.getBaseValue());
            double target = player == holder ? ENABLED_RECEIVE_RANGE : DISABLED_RECEIVE_RANGE;
            if (Double.compare(range.getBaseValue(), target) != 0) {
                range.setBaseValue(target);
            }
        }
    }

    private static boolean hasEgg(Player player) {
        // Storage includes hotbar and main inventory, but NOT ender chests,
        // containers in the world, dropped items, or cursor-carried items.
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG && item.getAmount() > 0) {
                return true;
            }
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return offHand.getType() == Material.DRAGON_EGG && offHand.getAmount() > 0;
    }

    private void restorePlayer(Player player) {
        Double original = originalRanges.remove(player.getUniqueId());
        if (original == null) {
            return;
        }
        AttributeInstance range = player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE);
        if (range != null) {
            range.setBaseValue(original);
        }
    }

    private void enableLocatorGameRules() {
        for (World world : getServer().getWorlds()) {
            enableLocatorGameRule(world);
        }
    }

    @SuppressWarnings("deprecation") // Old API field is retained across 1.21.6+.
    private void enableLocatorGameRule(World world) {
        // The global rule MUST remain true: false disables the bar even for
        // the egg holder. We restrict the bar with per-player attributes.
        if (!Boolean.TRUE.equals(world.getGameRuleValue(GameRule.LOCATOR_BAR))) {
            world.setGameRule(GameRule.LOCATOR_BAR, true);
        }
    }
}
