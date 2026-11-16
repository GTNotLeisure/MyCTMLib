# MyCTMLib

Connected textures and JSON models for Minecraft 1.7.10.

## JSON models

Edit `pack.mcmeta` at the root of an active resource pack using UTF-8. For an existing pack, merge the `myctmlib` section into its metadata.

```json
{
  "pack": {
    "pack_format": 1,
    "description": "MyCTMLib model settings"
  },
  "myctmlib": {
    "enableMinecraftJsonModels": {
      "redstone_torch": true,
      "repeater": true,
      "comparator": true
    },
    "enableBeeJsonModels": true,
    "enableAe2JsonModels": {
      "drive": true,
      "chest": true,
      "wireless_access_point": true,
      "charger": true,
      "inscriber": true,
      "advanced_inscriber": true,
      "molecular_assembler": true,
      "tiny_tnt": true,
      "crank": true,
      "quartz_fixture": true,
      "sky_chest": true,
      "smooth_sky_chest": true,
      "meteorite_compass": true,
      "matter_cannon": true,
      "network_tool": true,
      "advanced_network_tool": true,
      "color_applicator": true
    }
  }
}
```

- Set each model switch to `true` or `false`. Models default to disabled when no active pack enables them.
- Enable the pack under **Options > Resource Packs**, then press **F3 + T** to apply changes.
- Higher-priority packs override individual switches. An omitted switch can inherit a lower-priority value; use `false` to explicitly disable it. Server packs have the highest priority.
- Redstone switches cover placed models; unlit redstone torches keep their original rendering. The old redstone settings in `config/MyCTMLib.cfg` are no longer used.
- Bee and AE2 models require Forestry and AE2 respectively. Redstone and AE2 models fall back to their original rendering if loading fails.
