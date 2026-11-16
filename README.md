# MyCTMLib

MyCTMLib adds connected textures and JSON model support to Minecraft 1.7.10. JSON models change the appearance of items and blocks while preserving their redstone and machine behavior.

## Enabling and disabling JSON models

Each model group has its own settings:

| Models | Where to configure them | Default |
| --- | --- | --- |
| Redstone torches, repeaters, and comparators | MyCTMLib's mod configuration | Enabled |
| Forestry bee items | A resource pack's `pack.mcmeta` | Disabled unless an active resource pack enables them |
| AE2 blocks and items | A resource pack's `pack.mcmeta`, with a separate switch for each model | Disabled unless an active resource pack enables them |

After changing a setting, press **F3 + T** in-game to reload resources. Some keyboards also require **Fn**. Restarting the game also applies the changes.

### Redstone models

From the main menu, open **Mods > MyCTMLib > Config**. Change the redstone torch, repeater, or comparator JSON model setting, save, and reload resources.

You can also close the game and edit `config/MyCTMLib.cfg` in your game directory using UTF-8 encoding. The following settings disable all three redstone model replacements. Change a setting to `true` to enable it.

```text
general {
    B:redstoneTorchJsonModel=false
    B:redstoneRepeaterJsonModel=false
    B:redstoneComparatorJsonModel=false
}
```

If these settings already exist, edit their values in the existing `general` section.

The torch setting currently affects placed, lit redstone torches. Unlit redstone torches retain their original appearance. Repeater models cover all four delay settings, powered states, and locked states. Comparator models cover comparison mode, subtraction mode, and powered states. Disabling a setting restores the original rendering for that block.

### Bee and AE2 models

Install the corresponding mod to use its models. Their switches belong in `pack.mcmeta` at the root of an active resource pack.

If your resource pack already has a `pack.mcmeta`, keep its existing `pack` section and other settings, then add or update the `myctmlib` section shown below. If `myctmlib` already exists, merge the switches into that section.

You can also create a small resource pack dedicated to model settings in your game directory:

```text
resourcepacks/
  MyCTMLib-Models/
    pack.mcmeta
```

Save the following as `pack.mcmeta` using UTF-8 encoding, then enable the pack under **Options > Resource Packs**. This example enables bee models and all supported AE2 models:

```json
{
  "pack": {
    "pack_format": 1,
    "description": "MyCTMLib model settings"
  },
  "myctmlib": {
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

Use `true` to enable a model and `false` to disable it. These values must be lowercase and unquoted. Configure each model separately inside `enableAe2JsonModels`.

**Disable bee models:** Set `enableBeeJsonModels` to `false`. This switch covers drones, princesses, queens, larvae, and the related GUI and beekeeper upgrade displays that use these models.

**Disable an AE2 model:** Set its switch to `false`. For example, `"drive": false` restores the ME Drive's original appearance. To disable all AE2 model replacements, set every AE2 switch in the example to `false`.

| AE2 switch | Model |
| --- | --- |
| `drive` | ME Drive |
| `chest` | ME Chest |
| `wireless_access_point` | Wireless Access Point |
| `charger` | Charger |
| `inscriber` | Inscriber |
| `advanced_inscriber` | Advanced Inscriber |
| `molecular_assembler` | Molecular Assembler |
| `tiny_tnt` | Tiny TNT |
| `crank` | Wooden Crank |
| `quartz_fixture` | Quartz Fixture |
| `sky_chest` | Sky Stone Chest |
| `smooth_sky_chest` | Smooth Sky Stone Chest |
| `meteorite_compass` | Meteorite Compass |
| `matter_cannon` | Matter Cannon |
| `network_tool` | Network Tool |
| `advanced_network_tool` | Advanced Network Tool |
| `color_applicator` | Color Applicator |

### Using multiple resource packs

Packs nearer the top of the selected resource pack list have higher priority. A server-provided resource pack takes priority over locally selected packs.

Each switch uses the value explicitly set by the highest-priority pack that defines it. If a pack omits a switch, a lower-priority pack can still supply its value. To override another pack and disable a model, explicitly set its switch to `false` in a higher-priority pack.

### If a change does not take effect

- Make sure the resource pack is enabled and `pack.mcmeta` is at its root. For a ZIP pack, place `pack.mcmeta` directly at the ZIP root.
- Check the JSON syntax and switch names. Do not add a comma after the last entry in an object.
- Check whether a higher-priority pack overrides your settings, then press **F3 + T** to reload resources.
- Make sure the corresponding mod is installed. If a redstone or AE2 model or texture fails to load, its original renderer is used.
