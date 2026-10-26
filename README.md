# Leyline

A Fabric library mod for **Minecraft Java 1.21.11** that other mods build big, world-shaking effects on. It was split out of [Arcane Spells](https://github.com/CarsonKopec/ArcaneMod), which uses it for its spells, and is meant to be shared by other mods.

Leyline adds nothing to the game by itself: install it when a mod that needs it asks for it.

## What's in it

**Effect messages** (`dev.leyline.effect`, both sides; `dev.leyline.client.effect`, client)

- `Effect`: one visual event (where, which way, how strong, how old, and what it belongs to). The server sends it once, with `Effects.send`, to every player within 448 blocks who has that chunk loaded, and each client draws its own particles and lights from it, so individual particles never cross the network.
- `EffectType`: a kind of event. Register yours in `LeylineRegistries.EFFECT_TYPES` in your mod initializer and document what its fields mean. The registry is synced, so a client missing a type the server has is refused at login, naming it.
- `EffectRenderers`: draw effect types on the client, your own or another mod's; every renderer for a type runs, in registration order. One that throws is logged and switched off, rather than disconnecting the player.

```java
// Both sides, in your ModInitializer:
public static final EffectType BURST = Registry.register(LeylineRegistries.EFFECT_TYPES,
        Identifier.fromNamespaceAndPath("mymod", "burst"), new EffectType());

// Server, where it happens:
Effects.send(level, BURST, position, Vec3.ZERO, 1, 0, entity.getId());

// Client, in your ClientModInitializer:
EffectRenderers.register(BURST, (effect, level) -> { /* particles, lights */ });
```

**Terrain** (`dev.leyline.terrain`, both sides)

- `TerrainRules`: what an effect may do to terrain. Blocks only break where the player behind the effect could break them by hand (spawn protection, claims, the world border), never anything unbreakable or as blast resistant as obsidian. Also: vaporising blocks without drops, scorching a crater's walls to blackstone, basalt and magma, and an explosion calculator that breaks terrain but never hurts or pushes entities, so an effect that deals its own damage hits nothing twice.
- `Crater`: a crater's plan, an ellipsoid with a roughened rim, every block in it nearest the centre first plus the shell around its floor and walls.
- `Carving`: carves a crater over a few ticks rather than all at once, so a big one doesn't stall the server, optionally searing it afterwards.

Whether an effect may break blocks at all is the calling mod's decision (Adventure mode, a game rule of its own): read it once when the effect starts and pass it in as `mayBuild`.

**Effects** (`dev.leyline.client.fx`, client), drawn with [Nebulon](https://maven.riftrealms.de/releases/de/nexusrealms/nebulon/)

- `NebulonGuard`: every call into Nebulon goes through it. Nebulon is a beta library driving the GPU directly, so the first failure is logged once and switches Nebulon off for the session, for every mod using Leyline, rather than letting an exception disconnect the player; effects carry on without it.
- `Fx`: lights and Nebulon shapes that update in place rather than being respawned, particles that follow the Particles setting and stop beyond 192 blocks, easing, colour and random-direction helpers, and how close an effect is to the camera, for scaling screen effects by it.
- `Fx.Planes`: crisp flat shapes (rings, arcs, polygons, seals, camera-facing glows and halos) redrawn every tick, reusing their handles in drawing order. Nebulon's own magic circles add colour across their whole disc, so use these for lines and keep magic circles dim.
- `Fx.Tracker`, `Fx.Trackers` and `Fx.Animation`: an effect's running animations, keyed by the source id the server sent, dropped when the world changes. A mod groups its trackers in a `Trackers` and ticks it from its own client tick, in whatever order it needs.

**Screen effects** (`dev.leyline.client.screen`, client)

- `ScreenPass`: a full-screen shader pass drawn into the finished frame, after the world and before the hand and HUD (through Nebulon's post-processing), for what shapes and particles can't draw: light bent round a black hole, the colour drained from part of the world. It runs only while something asks for it each tick, so it costs nothing otherwise. With a shader pack (Iris) or VulkanMod, or once it or Nebulon has failed, it isn't available: check, and draw the effect another way.
- `ScreenPasses`: the pieces to draw one with: full-screen materials, textures sized to the frame, the draw itself, a copy of the frame to read while drawing over it (made with a draw, as copying textures directly has lost the colour on some drivers), and whether the GPU is Intel's integrated graphics, whose drivers have hung on long chains of passes.
- `leyline:screen_space.glsl`, a shader include: the ray through each pixel from the depth buffer, projecting points back onto the screen, ray-sphere hits, hashing and a heat-colour ramp. Import `minecraft:projection.glsl` and declare a uniform block holding `ViewMat` (the camera's rotation) first.
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

and declare it in your `fabric.mod.json`: `"leyline": "~0.1.0"`. Players install the Leyline JAR alongside your mod, along with Fabric API and [Nebulon 0.1.2-beta+mc1.21.11.20260928.19](https://maven.riftrealms.de/releases/de/nexusrealms/nebulon/0.1.2-beta+mc1.21.11.20260928.19/nebulon-0.1.2-beta+mc1.21.11.20260928.19.jar).

## Build

| Task | Command (Windows; use `./gradlew` elsewhere) |
| --- | --- |
| Build the mod JAR | `gradlew.bat build` → `build/libs/` |
| Publish to the local Maven repository | `gradlew.bat publishToMavenLocal` |

## License

CC0 1.0: see `LICENSE`.
