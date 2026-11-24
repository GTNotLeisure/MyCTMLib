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
    "enableProjectRedJsonModels": true,
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
    },
    "enableAutomagyJsonModels": {
      "redstone_inversion_torch": true
    },
    "enableGCRocketJsonModels": {
      "rocket_tier_1": true,
      "rocket_tier_2": true,
      "rocket_tier_3": true,
      "rocket_tier_4": true
    },
    "enableForestryJsonModels": {
      "bottler": true,
      "carpenter": true,
      "centrifuge": true,
      "fermenter": true,
      "moistener": true,
      "squeezer": true,
      "still": true,
      "escritoire": true
    }
  }
}
```

- Set each model switch to `true` or `false`. Models default to disabled when no active pack enables them.
- Enable the pack under **Options > Resource Packs**, then press **F3 + T** to apply changes.
- Higher-priority packs override individual switches. An omitted switch can inherit a lower-priority value; use `false` to explicitly disable it. Server packs have the highest priority.
- Models will fall back to their original rendering if loading fails.

## Credit

| Model and Texture Original Repositories | License |
| --- | --- |
| [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) | [LGPL 3.0](https://opensource.org/license/lgpl-3-0) | 
| [Forestry: CE](https://github.com/thedarkcolour/ForestryCE?tab=readme-ov-file)  | [LGPL 3.0](https://opensource.org/license/lgpl-3-0) |
| [Stellaris](https://github.com/st0x0ef/stellaris) | [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) |
