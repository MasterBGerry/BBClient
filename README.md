# BBClient

A Fabric 1.21.6 client mod that brings back 1.7/1.8 PvP mechanics. Designed to work with the BadBuck server fork.

## Features

### Combat
- **Sword Blocking** — right-click sword to block, with legacy 1.7/1.8 animation
- **No Attack Cooldown** — cooldown bar always full
- **Extended Reach** — 3.0 blocks
- **Extended Hitbox** — +0.1 entity hitbox expansion
- **CPS Particles** — crit/sharpness particles on every click, not just hits
- **Block & Hit** — attack while sword blocking

### Simultaneous Actions
- **Mine while eating/blocking**
- **Eat while mining**
- **Sword block priority** — blocks instead of interacting with non-interactive blocks

### Inventory
- **Offhand disabled** — no F-key swap, no offhand slot

### Visual
- **Potion Glint** — potions shimmer on hotbar
- **Bobber hidden** — when hooked on local player
- **Extended leaf particles** — 96 blocks range (vanilla = 32)
- **Hurt sound on rod hit**

### Network
- **Handshake** — sends version + features to server
- **Config sync** — receives server settings
- **Combat packets** — attack/sprint reset with distance + entity ID

### Anti-desync
- **Release block on GUI open**

## Requirements

- Minecraft 1.21.6
- Fabric Loader
- Fabric API

## Building

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## Server

This mod is designed to work with the [BadBuck](https://github.com/MasterBGerry/BadBuck) server fork. The server handles damage reduction, knockback, i-frames, and sprint reset mechanics server-side. The client handles animations, input priority, and visual features.
