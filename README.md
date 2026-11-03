# Leyline

A Fabric library mod for **Minecraft Java 1.21.11** that other mods build big, world-shaking effects on: its own renderer for smooth coloured lights, crisp rune circles, rings, lightning and beams, and full-screen shader passes, plus effect messages, terrain rules and screen shake. It was split out of [Arcane Spells](https://github.com/CarsonKopec/ArcaneMod), which uses it for its spells, and is meant to be shared by other mods. It needs only Fabric API.

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

**Rendering** (`dev.leyline.client.render`, client)

Leyline draws into the finished frame itself, once the whole world is drawn (terrain, entities, particles, weather and clouds) and before the hand and HUD: lights first, then shapes, then screen passes. Everything is hidden by terrain in front of it, belongs to the world it was made in, and goes when the player leaves that world. None of it runs with VulkanMod or while Iris runs a shader pack (`RenderCompat`); effects should still draw their particles.

- `Shapes`, `Planes`: shapes drawn from their distance fields, a whole batch in one draw with no vertex buffer. Edges are antialiased, and lines never thin to nothing far off: they stay about a pixel and a half wide, fading as they would have thinned, so a ring a hundred blocks away still shows. `Planes` draws rings, arcs, polygons, seals, glows and halos redrawn every tick (`begin`, draw, `end`). `Shapes.add` draws a shape until its handle is removed, and `Shapes.spawn` draws one for a few ticks, fading it out:
  - `RuneCircle`: rings, bands of runes drawn from a seed, and stars, with more for more layers, turning as it goes.
  - `Bolt`: lightning through any number of points, re-forked every tick, a white-hot core in a glow.
  - `Beam`: a strip turned about its axis to face the camera, plain or a sprite's flipbook.
- `Lights`: `PointLight`s and `BeamLight`s cast on the frame. Each pixel's position comes back from the depth buffer and its surface's facing from its neighbours'; a light adds its colour times the surface's own, fading smoothly to nothing at its radius, eased off as it grows so a strong light doesn't flatten a surface to white. They're smooth, with no dithering, and have no shadows: a light reaches through walls within its radius.
- `Rgba`: colours, premultiplied. Red, green and blue are the light a shape adds, and alpha how much it covers what's behind it, so an opaque line reads in daylight, scaling every channel fades a shape, and an alpha of 0 adds light without covering anything.
- `AfterWorld`: run your own drawing there, at a priority, with the frame's target, its depth, and the matrices the world was drawn with. `UniformBlock` writes a uniform block for a draw each frame.

```java
// A ring of runes under the caster, turning, until it's removed:
ShapeHandle<RuneCircle> runes = Shapes.add(new RuneCircle(feet, Fx.UP, 3, 0.05f, Rgba.rgb(0x9B4DFF), 4, seed, 0.4f));
// A violet light over it:
LightHandle<PointLight> glow = Lights.add(new PointLight(feet.add(0, 1, 0), Rgba.rgb(0x9B4DFF), 8, 1.5f));
// Lightning to the target, for six ticks:
Shapes.spawn(Bolt.between(staffTip, target, Rgba.rgb(0x7EE8FF)), 6);
```

**Effects** (`dev.leyline.client.fx`, client)

- `Fx`: shapes and lights that update in place rather than being respawned (`show`, `hide`, `light`, `unlight`), particles that follow the Particles setting and stop beyond 192 blocks, easing, colour and random-direction helpers, and how close an effect is to the camera, for scaling screen effects by it.
- `Fx.Tracker`, `Fx.Trackers` and `Fx.Animation`: an effect's running animations, keyed by the source id the server sent, dropped when the world changes. A mod groups its trackers in a `Trackers` and ticks it from its own client tick, in whatever order it needs. An animation that throws is logged, released and dropped; the rest carry on.
- `EffectGuard`: runs effect work where an exception would crash the game or disconnect the player, and logs each different failure once instead.

**Screen effects** (`dev.leyline.client.screen`, client)

- `ScreenPass`: a full-screen shader pass drawn into the finished frame, after the world and before the hand and HUD, for what shapes and particles can't draw: light bent round a black hole, the colour drained from part of the world. It runs only while something asks for it each tick, so it costs nothing otherwise. With a shader pack (Iris) or VulkanMod, or once it has failed, it isn't available: check, and draw the effect another way.
- `ScreenPasses`: the pieces to draw one with: full-screen pipelines (Minecraft's own `RenderPipeline`, on its full-screen triangle), textures sized to the frame, the draw itself, a copy of the frame to read while drawing over it (made with a draw, as copying textures directly has lost the colour on some drivers), and whether the GPU is Intel's integrated graphics, whose drivers have hung on long chains of passes.
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
	modImplementation "dev.leyline:leyline:0.1.1"
}
```

and declare it in your `fabric.mod.json`: `"leyline": "~0.1.1"`. Players install the Leyline JAR alongside your mod, along with Fabric API.

## Build

| Task | Command (Windows; use `./gradlew` elsewhere) |
| --- | --- |
| Build the mod JAR | `gradlew.bat build` → `build/libs/` |
| Publish to the local Maven repository | `gradlew.bat publishToMavenLocal` |
| Upload a release to Modrinth and CurseForge | `gradlew.bat publishMods`: see [docs/releasing.md](docs/releasing.md). Without tokens it's a dry run into `build/publishMods/` |
| Redraw the icon | `python tools/generate_icon.py` (needs `pip install numpy pillow`) |

## License

CC0 1.0: see `LICENSE`.
