# Changelog

Each version's section is also its release notes on Modrinth and CurseForge: `gradlew publishMods` uploads the section headed with the version being released.

## 0.1.0

The first release: the effect, terrain and screen-shader toolkit split out of Arcane Spells.

- **Effect messages:** `Effect`, `EffectType`, `Effects.send` and `EffectRenderers`. The server sends one compact message per visual event to players nearby, and each client draws it. The effect-type registry is synced.
- **Terrain:** `TerrainRules`, which only breaks blocks where the player behind the effect could break them by hand, plus `Crater` and `Carving`, which carve a crater over a few ticks.
- **Effects:** `Fx` (lights and shapes that update in place, particle helpers, `Planes`, trackers and animations) and `NebulonGuard`, which turns Nebulon off for the session after its first failure instead of disconnecting the player.
- **Screen effects:** `ScreenPass` and `ScreenPasses` for full-screen shader passes, the `leyline:screen_space.glsl` include, and `ScreenEffects` for camera shake, flashes, darkening, field of view, roll and camera pulls.
