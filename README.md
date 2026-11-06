# Hex Machina

A pixel-art action roguelike designed for three-player co-op, combining maze-like exploration, randomized loot, and challenging combat against robotic enemies.

> Working title. Feature descriptions reflect the planned game design and technical scope.

## Overview

The project centers on a team of human characters navigating interconnected rooms filled with robots, weapons, and unpredictable rewards. Each encounter is intended to test positioning, resource management, and teamwork as enemy difficulty increases throughout a run.

The visual direction uses 32 × 32 pixel sprites with a sense of depth in the environment and multidirectional movement. The goal is to pair a readable pixel-art style with fast combat and limited visibility that makes exploration feel tense.

## Core Gameplay Loop

1. Explore maze-like rooms and locate enemies, chests, and routes forward.
2. Fight robotic enemies while managing health, ammunition, and movement resources.
3. Collect randomized weapons, armor, and power-ups to strengthen your build.
4. Keep teammates alive and revive downed players before their timers expire.
5. Take on a boss in each of three stages, with keys awarded for defeating bosses.

## Gameplay Design

### Cooperative Survival

- Encounters designed around a challenging three-player experience.
- Human characters with basic customization and name tags for identification.
- A downed state that allows players to crawl while waiting for a teammate to revive them.
- Limited visibility that encourages coordination and awareness.

### Combat and Equipment

- Ranged and melee weapons spanning conventional and fantasy designs.
- Ammunition management for applicable weapons.
- Armor, buffs, and weapon scaling to support progression during a run.
- Randomized chest rewards that encourage players to adapt their equipment choices.
- Crosshair aiming and sprinting as part of the planned combat controls.

### Enemies and Encounters

- Multiple robotic enemy types with increasing difficulty.
- Stronger variants of regular enemies serving as minibosses.
- Three stage bosses that anchor the run's progression.

## Technical Scope

Development is organized into sprints around interconnected gameplay systems:

| Area | Planned Work |
| --- | --- |
| Multiplayer | Host-based networking and cooperative player interactions |
| Player systems | Multidirectional movement, aiming, health, sprinting, and revival |
| Enemy AI | Pathfinding, behavior states, enemy variants, and boss encounters |
| Combat | Collision detection, projectiles, melee and ranged weapons, and damage handling |
| Progression | Chest rewards, equipment, buffs, and weapon scaling |
| World and presentation | Room layouts, pixel sprites, visibility, and gameplay UI |

## Roadmap

### Core Playable Experience

Establish a playable foundation with a basic map, responsive movement, enemy pathfinding, collision, health, and a small weapon selection. Build on that foundation with chest rewards, additional enemy types, boss encounters, cooperative networking, and the revival system.

### Future Expansion

- Character classes with distinct starting weapons, buffs, and item bonuses.
- Procedurally generated maps.
- Shops, traps, and portals.
- More advanced enemy and boss behaviors.
- Optional friendly fire.
- Endless mode and additional game modes, including PvP.
- Story and world-building elements.

Expansion features remain stretch goals, with priority given to a cohesive core combat and co-op experience.
