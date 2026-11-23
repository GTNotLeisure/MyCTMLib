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
- Redstone switches cover placed models; unlit redstone torches keep their original rendering. The old redstone settings in `config/MyCTMLib.cfg` are no longer used.
- Bee and AE2 models require Forestry and AE2 respectively. Redstone and AE2 models fall back to their original rendering if loading fails.

## Forestry machines

The Forestry machines and the escritoire can be drawn from JSON models and textures bundled with this mod instead of
Forestry's own tile entity renderers.

- Covered blocks: `bottler`, `carpenter`, `centrifuge`, `fermenter`, `moistener`, `squeezer`, `still` and `escritoire`.
- Every block is switched individually through `enableForestryJsonModels` in `pack.mcmeta`; blocks that are not enabled
  keep their original Forestry rendering until the resource packs are reloaded.
- The tank overlays follow the fill level of the machine and are tinted with the colour of the fluid inside.
- A model or texture that cannot be loaded leaves the original Forestry renderer in place.
- The models live in `assets/myctmlib/models/block/forestry` and are registered in
  `assets/myctmlib/forestry/models.json`. Regenerate them from an exported Blockbench folder with
  `python tools/import_forestry_models.py <folder>`.
