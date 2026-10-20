# HimeLib

The animation engine of Hime (Fabric 1.21.1): a fork of [GeckoLib](https://github.com/bernie-g/geckolib) 4.8.3, under
its own name (package `com.cuddly.himelib`, mod id and namespace `himelib`) so it runs beside the real GeckoLib that
other mods depend on. Model and animation files are GeckoLib's, unchanged (`geo/*.geo.json`, `animations/*.animation.json`).

GeckoLib is MIT licensed, by Gecko, Eliot, AzureDoom, DerToaster, Tslat and Witixin: see `LICENSE_GeckoLib`.

## Changes from GeckoLib 4.8.3

- Keyframes are read sorted by time (Blockbench can write integer-like times first).
- `GeoBone.setHiddenWithoutHidingChildren`: hides a bone's own cubes, not its children.
- Runtime assets: `RuntimeLoader` bakes models and animations from JSON in memory, and
  `GeckoLibCache.setRuntimeAssets` puts them in beside the resource packs' (looked up first, kept through a
  resource reload, swapped whole). Hime brings its girls this way, from its own packs.

## Build

`./gradlew publishToMavenLocal`, then Hime depends on `com.cuddly:himelib` (and bundles it).

Forked for Hime from HeartLib (the same GeckoLib 4.8.3 fork, made for Unison).
