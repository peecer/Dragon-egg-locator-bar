# Dragon Egg Locator

A lightweight, server-side Paper plugin for **Minecraft Java 1.21.6+**. No client mods or permissions are needed.

## Rules

- Nobody can use the vanilla locator bar unless they have the **Dragon Egg** (`minecraft:dragon_egg`) in their normal inventory or offhand.
- Exactly **one** online player can use the locator bar. Whoever already has access keeps it as long as they retain an egg. If duplicate eggs exist, the plugin picks one holder rather than enabling the locator bar for everyone carrying a copy.
- The holder can see normal vanilla player waypoints from other players. Others cannot use the bar to track anybody.
- Dropping, placing, losing, or storing the egg in a chest or ender chest disables access. The next person to pick it up gains access automatically.
- Updates happen once per server tick (typically within 50 ms), including when inventory contents change via commands, creative mode, or container movement.
- The plugin enables the world's `locator_bar` / `locatorBar` game rule automatically. **Do not set the game rule to false**: this would prevent even the egg holder from seeing the bar. The plugin instead sets `waypoint_receive_range` to 0 for everyone except the egg holder.
- Sneaking, invisibility, and head disguises still hide players from the vanilla locator bar, as usual.

## Install

1. Use a **Paper** or compatible server running Minecraft Java **1.21.6 or newer** (Java 21+; use the Java version required by your server).
2. Download the `DragonEggLocator-1.0.0.jar` from a successful GitHub Actions build, or build it yourself (see below).
3. Put the JAR into the server's `plugins/` directory.
4. Restart the server. That's it.

## Build

```bash
mvn clean package
```

Output: `target/DragonEggLocator-1.0.0.jar`.
The Paper API is `provided`; the final JAR does not bundle it.

## Design and limitations

The locator bar is enabled globally via the game rule, but *receiving* is restricted per player using `Attribute.WAYPOINT_RECEIVE_RANGE`. **Do not disable waypoint transmission for non-holders**, or the egg holder would not be able to locate them. This plugin does not use packet hacks or NMS internals.

The Dragon Egg must be in regular player inventory storage or the offhand; an egg on the cursor, inside a shulker box, or in an ender chest does not count. If the holder logs out, another online egg holder is picked. On logout and clean plugin shutdown, any altered receive ranges are restored to their original values. On sudden server crashes, player attribute values may persist until the plugin is enabled again or an admin resets them.

The plugin actively maintains the world's locator-bar game rule as `true` while enabled. It may conflict with other plugins or data packs that modify the same rule or the waypoint receive-range attribute. Supports standard Paper/Bukkit scheduling, not Folia.
