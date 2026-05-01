# Sudarshan Chakra Mod — Fabric 1.21.1

A Minecraft Fabric mod that adds the legendary **Sudarshan Chakra** — Lord Vishnu's divine spinning disc weapon.

---

## Features

| Feature | Details |
|---|---|
| **Throwable** | Right-click to throw. Returns to the player automatically. |
| **Spinning 3D model** | Disc rotates 360° in flight with a tilted orientation. |
| **AoE damage** | 12 direct + 6 splash damage within 3.5 block radius. |
| **Fire on hit** | Sets all affected mobs on fire for 4 seconds. |
| **Particle trail** | Golden flame + end-rod sparks while in flight. |
| **Returns after hit** | Flies back to the thrower after the first hit or after 6 seconds. |
| **One at a time** | Only one chakra can be in the air — 2 second cooldown. |

---

## Crafting Recipe

```
  [Gold]
[Gold] [Blaze Rod] [Gold]
  [Gold]
```

---

## Setup

### Prerequisites
- JDK 21+
- Fabric Loader 0.16+
- Fabric API for 1.21.1

### Build
```bash
./gradlew build
```
The mod JAR will appear in `build/libs/`.

### Adding custom sounds (optional)
Place `.ogg` files at:
```
src/main/resources/assets/sudarshanmod/sounds/
  chakra_throw.ogg
  chakra_hit.ogg
  chakra_return.ogg
```
Without these, Minecraft will silently skip the sound — no crash.

### Texture
The `sudarshan_chakra.png` is auto-generated. Replace it with your own 16×16 or 64×64 pixel art for a custom look.

---

## File Structure

```
src/
├── main/java/com/sudarshanmod/
│   ├── SudarshanMod.java          ← Registers item, entity, sounds
│   ├── item/
│   │   └── SudarshanChakraItem.java  ← Right-click throw, cooldown
│   └── entity/
│       └── ChakraEntity.java      ← Flight, AoE hit, fire, return logic
│
├── client/java/com/sudarshanmod/client/
│   ├── SudarshanModClient.java    ← Registers renderer
│   └── renderer/
│       └── ChakraEntityRenderer.java ← Spinning disc rendering
│
└── main/resources/
    ├── fabric.mod.json
    ├── assets/sudarshanmod/
    │   ├── models/item/sudarshan_chakra.json
    │   ├── textures/item/sudarshan_chakra.png
    │   ├── lang/en_us.json
    │   └── sounds.json
    └── data/sudarshanmod/
        └── recipes/sudarshan_chakra.json
```

---

## Extending the mod

**Enchantment support** — Add a `Loyalty`-like enchantment to reduce return time.

**Multiple chakras** — Track per-player UUID in entity NBT and allow multiple simultaneous.

**Boss weapon** — Spawn a Chakra-throwing Vishnu Guardian mob using `HostileEntity`.

**Creative tab** — Register an `ItemGroup` to put the chakra in a divine weapons tab.
