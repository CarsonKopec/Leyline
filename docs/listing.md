# Leyline: store page

The text for the Modrinth and CurseForge project pages. Paste the summary into the site's summary field and everything under "Description" into its description editor (both take Markdown).

## Summary

A rendering library for big effects: smooth coloured lights, crisp rune circles, lightning and beams, shader passes, effect messages and shared terrain rules.

## Description

**Leyline is a library mod.** It adds nothing to the game by itself. Install it when a mod that needs it, such as [Arcane Spells](https://github.com/CarsonKopec/ArcaneMod), asks for it, on the server and on every client.

It gives mods the groundwork for big effects:

- **Its own renderer:** rune circles, rings, seals, lightning and beams that stay crisp at any distance and read in daylight, smooth coloured lights with no speckle, and full-screen shader passes for effects like black holes. No shader pack is needed.
- **Effect messages:** the server sends one small message per visual event to players nearby, and each client draws its own particles and lights from it. Individual particles never cross the network.
- **Terrain rules:** effects only break blocks where the player behind them could break them by hand, which respects spawn protection, claims and the world border. They never break unbreakable blocks or anything as blast resistant as obsidian.
- **Craters:** carved over a few ticks within a budget, so a big one doesn't stall the server, and optionally scorched to blackstone, basalt and magma.
- **Screen effects:** camera shake, flashes, darkening and camera pulls. These follow the Screen Effect Scale, FOV Effects and Hide Lightning Flashes settings.

### Before you install

- **It needs:** Fabric API.
- **Shader packs (Iris) and VulkanMod:** Leyline's own drawing switches off under them, and effects fall back to particles.
- **For mod developers:** the API is documented on [GitHub](https://github.com/CarsonKopec/Leyline).
