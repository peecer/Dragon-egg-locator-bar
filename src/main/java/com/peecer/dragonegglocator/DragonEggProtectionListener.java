package com.peecer.dragonegglocator;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Protects dragon eggs from natural item despawning, damage (including fire,
 * lava and explosions), and explosions that destroy placed dragon-egg blocks.
 * This intentionally does not stop players from breaking, picking up, placing,
 * or transferring the egg in the normal way.
 */
public final class DragonEggProtectionListener implements Listener {

    private static boolean isDragonEgg(Item item) {
        return item.getItemStack().getType() == Material.DRAGON_EGG;
    }

    private static void protect(Item item) {
        if (!isDragonEgg(item)) {
            return;
        }
        // Paper's unlimited lifetime flag persists with the item entity.
        item.setUnlimitedLifetime(true);
        // Avoid direct entity damage even when something does not fire a
        // cancellable event. Regular item pickup still works normally.
        item.setInvulnerable(true);
        item.setFireTicks(0);
    }

    /** Handle eggs dropped before a plugin reload or in an unloaded chunk. */
    public void protectLoadedEggs(Iterable<World> worlds) {
        for (World world : worlds) {
            for (Chunk chunk : world.getLoadedChunks()) {
                protectItemsInChunk(chunk);
            }
        }
    }

    private static void protectItemsInChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item item) {
                protect(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        protect(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        protectItemsInChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (isDragonEgg(event.getEntity())) {
            event.setCancelled(true);
            protect(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && isDragonEgg(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Item item && isDragonEgg(item)) {
            event.setCancelled(true);
            item.setFireTicks(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> block.getType() == Material.DRAGON_EGG);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> block.getType() == Material.DRAGON_EGG);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
        }
    }
}
