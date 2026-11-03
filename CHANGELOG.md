# Changelog

Each version's section is also its release notes on Modrinth and CurseForge: `gradlew publishMods` uploads the section headed with the version being released.

## 0.1.1

No changes to the mod itself: 0.1.0 republished, telling Modrinth it runs on both the client and the server.

## 0.1.0

The first release: the rendering, effect and terrain toolkit Arcane Spells' spells are drawn with, made into a library. It needs only Fabric API.

- **Its own renderer**, drawing into the finished frame once the whole world is drawn and before the hand:
  - **Shapes** (`Shapes`, `Planes`): rune circles, rings, arcs, polygons, seals, glows, halos, lightning and beams, drawn from their distance fields in one batched draw. Crisp at any distance, legible in daylight, and hidden by terrain in front of them.
  - **Lights** (`Lights`): smooth coloured point and beam lights, lighting each surface by its own colour and facing, with no dithering.
  - **Screen passes** (`ScreenPass`, `ScreenPasses`): full-screen shader passes that read the world's depth, for what shapes can't draw. With the `leyline:screen_space.glsl` include.
- **Effect messages:** `Effect`, `EffectType`, `Effects.send` and `EffectRenderers`. The server sends one compact message per visual event to players nearby, and each client draws it. The effect-type registry is synced.
- **Terrain:** `TerrainRules`, which only breaks blocks where the player behind the effect could break them by hand, plus `Crater` and `Carving`, which carve a crater over a few ticks.
- **Effects:** `Fx` (shapes and lights that update in place, particle helpers, trackers and animations) and `EffectGuard`, which logs a failing effect instead of letting it crash the game or disconnect the player.
- **Screen effects:** `ScreenEffects` for camera shake, flashes, darkening, field of view, roll and camera pulls, all following the accessibility settings.
