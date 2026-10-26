# Leyline

A Fabric library mod for **Minecraft Java 1.21.11** that other mods build big, world-shaking effects on. It was split out of [Arcane Spells](https://github.com/CarsonKopec/ArcaneMod), which uses it for its spells, and is meant to be shared by other mods.

Leyline adds nothing to the game by itself: install it when a mod that needs it asks for it.

## What's in it

**Terrain** (`dev.leyline.terrain`, both sides)

- `TerrainRules`: what an effect may do to terrain. Blocks only break where the player behind the effect could break them by hand (spawn protection, claims, the world border), never anything unbreakable or as blast resistant as obsidian. Also: vaporising blocks without drops, scorching a crater's walls to blackstone, basalt and magma, and an explosion calculator that breaks terrain but never hurts or pushes entities, so an effect that deals its own damage hits nothing twice.
- `Crater`: a crater's plan, an ellipsoid with a roughened rim, every block in it nearest the centre first plus the shell around its floor and walls.
- `Carving`: carves a crater over a few ticks rather than all at once, so a big one doesn't stall the server, optionally searing it afterwards.

Whether an effect may break blocks at all is the calling mod's decision (Adventure mode, a game rule of its own): read it once when the effect starts and pass it in as `mayBuild`.

**Screen effects** (`dev.leyline.client.screen`, client)

- `ScreenEffects`: camera shake, a flash, a coloured darkening of the view, a wider or narrower field of view, a roll, and a pull of the camera towards something, all for the view only (the player's aim is untouched). Shake, roll and pull follow Screen Effect Scale, the field of view follows FOV Effects, and there is no flash with Hide Lightning Flashes on. Ask for each every tick it should last; Leyline ticks and clears them.

## Using it

Leyline isn't on a public Maven repository yet. Build it and put it in your local one:

```
gradlew.bat publishToMavenLocal
```

Then, in your mod:

```groovy
repositories {
	mavenLocal() { content { includeGroup 'dev.leyline' } }
}

dependencies {
	modImplementation "dev.leyline:leyline:0.1.0"
}
```

and declare it in your `fabric.mod.json`: `"leyline": "~0.1.0"`. Players install the Leyline JAR alongside your mod, along with Fabric API.

## Build

| Task | Command (Windows; use `./gradlew` elsewhere) |
| --- | --- |
| Build the mod JAR | `gradlew.bat build` → `build/libs/` |
| Publish to the local Maven repository | `gradlew.bat publishToMavenLocal` |

## License

CC0 1.0: see `LICENSE`.
