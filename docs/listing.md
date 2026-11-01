# Leyline: store page

The text for the Modrinth and CurseForge project pages. Paste the summary into the site's summary field and everything under "Description" into its description editor (both take Markdown).

## Summary

A library for big, world-shaking effects: effect messages, shared terrain rules, budgeted craters, safe Nebulon visuals, full-screen shader passes and screen shake.

## Description

**Leyline is a library mod.** It adds nothing to the game by itself. Install it when a mod that needs it, such as [Arcane Spells](https://github.com/CarsonKopec/ArcaneMod), asks for it, on the server and on every client.

It gives mods the groundwork for big effects:

- **Effect messages:** the server sends one small message per visual event to players nearby, and each client draws its own particles and lights from it. Individual particles never cross the network.
- **Terrain rules:** effects only break blocks where the player behind them could break them by hand, which respects spawn protection, claims and the world border. They never break unbreakable blocks or anything as blast resistant as obsidian.
- **Craters:** carved over a few ticks within a budget, so a big one doesn't stall the server, and optionally scorched to blackstone, basalt and magma.
- **Safe visuals:** every call into Nebulon is guarded. If Nebulon fails, Leyline logs it once and switches it off for the session, and the player stays connected.
- **Screen effects:** full-screen shader passes, and camera shake, flashes, darkening and camera pulls. These follow the Screen Effect Scale, FOV Effects and Hide Lightning Flashes settings.

### Before you install

- **It needs these mods:** Fabric API and Nebulon. Nebulon 0.1.2-beta+mc1.21.11.20260928.19 is published by NexusRealms.
- **For mod developers:** the API is documented on [GitHub](https://github.com/CarsonKopec/Leyline).
