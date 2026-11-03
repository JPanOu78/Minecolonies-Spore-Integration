# 🍄 MineColonies x Spore: Integration Bridge

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/ModLoader-NeoForge-orange.svg)](https://neoforged.net/)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

A dedicated compatibility bridge for **Minecraft 1.21.1 (NeoForge)** connecting **MineColonies** with **Fungal Infection: Spore**.

In vanilla environments, MineColonies guards do not recognize Spore infected mobs as threats until attacked, and spore mobs can freely spawn inside colony borders. This integration bridge turns your colony into an active, fortified defense network: guards proactively attack infected threats on sight, colony territory is shielded from natural hostile spawns, and citizens experience an immersive infection system with natural recovery and hospital cryogenic treatment.

---

## 🌉 Architecture & Bridge Overview

```
┌───────────────────────────────────┐                  ┌───────────────────────────────────┐
│            MineColonies           │                  │     Fungal Infection: Spore       │
│  - Colony Territory Borders       │ ◄──────────────► │  - 80+ Infected Hostile Species   │
│  - Guard AI & Combat Routines     │   INTEGRATION    │  - Mycelium Contagion & Spread    │
│  - Citizen Health & Bed Sleeping  │      BRIDGE      │  - Spore Fungal Environment       │
│  - Hospital Building & Doctors    │                  │  - Infected Villager Conversions  │
└───────────────────────────────────┘                  └───────────────────────────────────┘
```

---

## ✨ Features

### 1. ⚔️ Proactive Guard Threat Detection
*Bridges Spore entities into MineColonies hostile targeting tags.*
* **Automatic Tag Injection:** Injects over 80+ Spore infected entities directly into the `#minecolonies:hostile` tag registry.
* **Proactive Engagement:** Guards of all professions (Knights, Rangers, Druids, Mages, Gunners) detect and charge Spore infected creatures on sight from distance without needing to take damage first.
* **Coordinated Defense:** Eliminates passive guard behavior when defending town gates, walls, and borders against creeping infections.

---

### 2. 🛡️ Colony Territory Spawn Blocker
*Protects claimed colony territory from hostile monster and spore mob world generation.*
* **Sanctuary Borders:** Intercepts `FinalizeSpawn` events and completely blocks hostile monsters and Spore mobs from naturally spawning within claimed colony borders.
* **Conversion Whitelist:** Intelligently exempts conversion spawns (`MobSpawnType.CONVERSION`, `SporeInfectionConversion`), allowing valid game transformations (such as deceased infected citizens turning into infected entities) to still function properly.

---

### 3. 🍄 Citizen Mycelium Infection System
*Enables citizens to contract the fungal plague while preserving smooth gameplay and sleep.*
* **Contagion Vectors:** Citizens can catch the Mycelium infection (`spore:mycelium_ef`) from Spore mob combat or by standing on fungal spore blocks.
* **Zero Damage / No Bed Ejection:** Periodic `mycelium_overtake` tick damage is cancelled for citizens via `LivingIncomingDamageEvent`. This prevents citizens from flinching/stuttering while walking and ensures they sleep peacefully in beds without being violently bounced out onto the floor.
* **Tragic Conversion on Death:** If an infected citizen dies while having active Mycelium, they succumb to the fungus and transform into an **Infected Villager** (`spore:inf_villager`), retaining their original citizen name (e.g., *"Infected John"*).

---

### 4. 🏥 Dual Cure Pathways (Natural & Medical)
*Balanced recovery mechanics allowing colonies to survive with or without an established hospital.*
* **Natural Recovery:** The Mycelium status naturally runs its course (~60 seconds) and clears on its own—visiting a hospital is **not** mandatory to survive.
* **Hospital Treatment with Ice:**
  * Registered as an official MineColonies disease (`minecolonies:mycelium`).
  * **Hospital Stock Requirement:** Ice (`minecraft:ice`) is automatically added to the Hospital's required medical supplies list; couriers will fetch and stock ice in the hospital chest.
  * When an infected citizen enters the Hospital or is treated by a Doctor (`JobHealer`), the doctor consumes **1 Ice** and cures them **instantly**, triggering snowflake and cure particle effects!
* **Player First Aid:** Players can right-click an infected citizen with 1 Ice to cure them immediately on the spot.

---

## 📦 Requirements & Tested Versions

| Mod | Tested Version | Required? |
| :--- | :--- | :--- |
| **Minecraft** | `1.21.1` | **Required** |
| **NeoForge** | `21.1.249` (or `21.1.84`+) | **Required** |
| **[MineColonies](https://modrinth.com/mod/minecolonies)** | `1.1.1403-1.21.1` | **Required** |
| **[Structurize](https://modrinth.com/mod/structurize)** | `1.0.832-1.21.1` | **Required** (MineColonies Core) |
| **[BlockUI](https://modrinth.com/mod/blockui)** | `1.0.209-1.21.1` | **Required** (MineColonies Core) |
| **[Fungal Infection: Spore](https://modrinth.com/mod/spore)** | `2.2.0j` (`spore_1.21.1_2.2.0j_neo.jar`) | **Required** |

---

## ⚙️ Data Pack & Tag Structure

The mod supplies data pack resources ready for customization:
* **Hostile Entities Tag:** `data/minecolonies/tags/entity_type/hostile.json` (defines all Spore mob types recognized by guards).
* **Mycelium Disease Definition:** `data/minecolonies/colony/diseases/mycelium.json` (defines cure item requirement `minecraft:ice` and rarity 0).

---

## ❓ Frequently Asked Questions (FAQ)

#### Q: Do citizens die if they don't go to the hospital?
**A:** No! The Mycelium infection will naturally expire and disappear after its standard duration (~60 seconds). Citizens only transform if they die from enemy attacks or other hazards while infected.

#### Q: Why does the Doctor use Ice to cure Mycelium?
**A:** Fungal mycelium is biologically sensitive to cold temperatures. In Lore, Ice acts as a cryogenic agent that halts fungal germination immediately.

#### Q: Can Spore mobs still spawn inside my colony?
**A:** Natural mob spawning is completely blocked inside claimed borders. However, if an infected citizen dies inside town, they will transform into an infected villager, creating exciting internal containment challenges!

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
Developed by **mrzero784**.

