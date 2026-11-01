# 🍄 MineColonies x Spore Integration

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://modrinth.com/)
[![NeoForge](https://img.shields.io/badge/Modloader-NeoForge-orange.svg)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

> **Unite your colony's defenses against the Fungal Infection! Equips guards to target Spore mobs proactively, shields colony territory from natural spawns, and adds immersive citizen infection & hospital cure mechanics.**

---

## 📖 Overview

**MineColonies x Spore Integration** is a dedicated bridge mod for **Minecraft 1.21.1 (NeoForge)** that connects **MineColonies** with the horror survival mod **Fungal Infection: Spore**.

In vanilla setups, MineColonies guards do not recognize Spore infected monsters as hostile until attacked, and spore mobs can spawn freely inside colony borders. This mod completely overhaul that dynamic: guards charge Spore monsters on sight, territory is shielded from natural spawns, and citizens can catch the fungal infection—with realistic symptoms, natural recovery, and emergency treatment at the Hospital using Ice!

---

## ✨ Features

### ⚔️ 1. Proactive Guard Targeting
* **Immediate Threat Detection:** Automatically registers over 80+ Spore infected entities into `#minecolonies:hostile`.
* **Zero Hesitation:** All guard professions (Knights, Rangers, Druids, Mages, Gunners) aggressively target and eliminate Spore mobs on sight from afar without needing to take damage first.

---

### 🛡️ 2. Colony Border Spawn Blocker
* **Protected Sanctuary:** Completely blocks hostile monsters and Spore infected mobs from naturally spawning within claimed colony borders.
* **Infection Exemption:** Smartly permits infection-based conversions (`MobSpawnType.CONVERSION`), ensuring valid game mechanics (such as deceased citizens turning into infected entities) still function seamlessly.

---

### 🍄 3. Citizen Mycelium Infection System
* **Dynamic Contagion:** Citizens can contract the Mycelium infection (`spore:mycelium_ef`) through combat with Spore mobs or exposure to fungal blocks.
* **No Sleep Disruption / Stuttering:** Cancels the periodic `mycelium_overtake` tick damage for citizens! This ensures infected citizens walk smoothly without flinching and can sleep peacefully in beds without being bounced out onto the floor.
* **Tragic Death Transformation:** If an infected citizen succumbs and dies while having Mycelium, they transform into an `Infected Villager` (`spore:inf_villager`), preserving their original citizen name!

---

### 🏥 4. Dual Cure Mechanics (Natural Recovery & Hospital Treatment)
* **Natural Recovery:** The Mycelium status naturally runs its course (~60 seconds) and clears on its own—visiting a hospital is **not** mandatory to survive.
* **Doctor Instant Cure with Ice:**
  * Registered as an official MineColonies disease (`minecolonies:mycelium`).
  * **Hospital Requirement:** Ice (`minecraft:ice`) is automatically added as a required medical supply for the Hospital.
  * When an infected citizen enters the Hospital or is treated by a Doctor (`JobHealer`), the doctor consumes **1 Ice** and cures them **instantly** with snowflake and cure particle effects!
* **Player First Aid:** Players can also right-click an infected citizen with 1 Ice to cure them immediately on the spot.

---

## 📦 Requirements & Compatibility

| Mod | Tested Version | Required? |
| :--- | :--- | :--- |
| **Minecraft** | `1.21.1` | **Required** |
| **NeoForge** | `21.1.249` (or `21.1.84`+) | **Required** |
| **[MineColonies](https://modrinth.com/mod/minecolonies)** | `1.1.1403-1.21.1` | **Required** |
| **[Structurize](https://modrinth.com/mod/structurize)** | `1.0.832-1.21.1` | **Required** (MineColonies Core) |
| **[BlockUI](https://modrinth.com/mod/blockui)** | `1.0.209-1.21.1` | **Required** (MineColonies Core) |
| **[Fungal Infection: Spore](https://modrinth.com/mod/spore)** | `2.2.0j` (`spore_1.21.1_2.2.0j_neo.jar`) | **Required** |

---

## ❓ Frequently Asked Questions (FAQ)

#### Q: Do citizens die if they don't go to the hospital?
**A:** No! The Mycelium infection will naturally expire and disappear after its standard duration (~60 seconds). Citizens only transform if they are killed by enemies or other damage while infected.

#### Q: Why does the Doctor use Ice to cure Mycelium?
**A:** Fungal mycelium is vulnerable to freezing temperatures. Ice acts as a cryogenic treatment that halts spore germination immediately.

#### Q: Can Spore mobs still spawn inside my colony?
**A:** Natural mob spawning is completely blocked inside claimed borders. However, if an infected citizen dies, they will turn into an infected villager on the spot, creating dynamic internal emergencies!

---

## 🇹🇭 คำอธิบายภาษาไทย (Thai Description)
ม็อดเชื่อมระบบระหว่าง **MineColonies** และ **Fungal Infection: Spore**:
* **ยามโจมตีม็อบ Spore อัตโนมัติ:** ทหารยามทุกอาชีพจะมองเห็นและวิ่งเข้าโจมตีม็อบ Spore ทุกตัวทันทีโดยไม่ต้องรอโดนตีก่อน
* **บล็อกม็อบเกิดในเขตเมือง:** ป้องกันม็อบเกิดเองตามธรรมชาติในเขตเมือง แต่ยังยอมรับการกลายร่างจากการติดเชื้อ
* **ระบบติดเชื้อ Mycelium:** Citizen สามารถติดเชื้อราได้ แต่ปิดดาเมจกระตุกทำให้เดินไม่ชะงักและนอนเตียงไม่เด้งหลุด
* **การรักษา 2 ทางเลือก:** เชื้อราสามารถหายไปเองได้ตามเวลาปกติ (~60 วินาที) หรือไปให้หมอที่โรงพยาบาลรักษาทันทีด้วยน้ำแข็ง 1 ก้อน (ผู้เล่นสามารถถือน้ำแข็งคลิกขวารักษาได้เองด้วย)
* **กลายร่างเมื่อตาย:** หาก Citizen เสียชีวิตระหว่างติดเชื้อ จะกลายร่างเป็น Infected Villager พร้อมคงชื่อเดิมของ Citizen ไว้

