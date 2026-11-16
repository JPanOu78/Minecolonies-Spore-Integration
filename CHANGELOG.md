# Changelog - Minecolonies x Spore Integration

## [1.0.2] - Spore Combat Research Tree & Mycelium Immunity

### Added & Changed
- **Unified Level 1 Research (`Spore Warfare I`):** Grants both +25% Spore Damage and +25% Spore Defense at Level 1, then splits into 2 branches at Level 2:
  - **Spore Slayer II–V:** Bonus damage vs. Spore mobs (**+25% / +50% / +75% / +100% / +125%**).
  - **Spore Resistance II–V:** Bonus defense vs. Spore mobs (**+25% / +50% / +75% / +100% / +125%**).
- **Mycelium Immunity (Level 3):** Unlocked from *Spore Resistance II*; grants all citizens permanent 100% immunity to `spore:mycelium` infection.

### Fixed
- Fixed `Mycelium Immunity` parent research link so it properly appears in the University Combat branch.

## [1.0.1] - Safe Spawning & Raid Preservation Update

### Fixed
- Whitelisted `minecolonies` raiders, `MobSpawnType.EVENT` spawns, and `GourmetCustomer` entities so the colony spawn blocker no longer cancels raids or deletes restaurant customers.
- Made `spore` an optional soft dependency for standalone safety.
