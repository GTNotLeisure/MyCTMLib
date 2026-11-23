import json
import shutil
import sys
from pathlib import Path

MACHINES = {
    "bottler": (True, False),
    "carpenter": (True, False),
    "centrifuge": (False, False),
    "fermenter": (True, True),
    "moistener": (True, False),
    "squeezer": (False, True),
    "still": (True, True),
}

TANK_LEVELS = 5
ROOT = Path(__file__).resolve().parent.parent
ESCRITOIRE_MODEL = Path(__file__).resolve().parent / "models/escritoire.json"


def namespace(textures: dict) -> dict:
    empty = "myctmlib:blocks/machines/tank_empty"
    return {key: empty for key in textures}


def prefix_references(model: dict) -> None:
    keys = set(model.get("textures", {}))
    for element in model.get("elements", []):
        for face in element.get("faces", {}).values():
            texture = face.get("texture")
            if texture in keys:
                face["texture"] = "#" + texture


def dump(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main() -> None:
    source = Path(sys.argv[1] if len(sys.argv) > 1 else "D:/Document/WorkBuddy/default/Forestry")
    output = Path(sys.argv[2] if len(sys.argv) > 2 else ROOT / "src/main/resources/assets/myctmlib")

    models = output / "models/block/forestry"
    textures = output / "textures"

    texture_source = source / "textures/blocks"
    texture_target = textures / "blocks"
    shutil.copytree(texture_source, texture_target, dirs_exist_ok=True)

    machine = json.loads((source / "models/machines/base_machine.json").read_text(encoding="utf-8"))
    machine["textures"] = namespace(machine["textures"])
    machine.pop("groups", None)
    prefix_references(machine)
    dump(models / "machine.json", machine)

    manifest = {"models": {}}
    for name, (resource, product) in MACHINES.items():
        dump(
            models / "machines" / f"{name}.json",
            {
                "parent": "myctmlib:block/forestry/machine",
                "textures": {
                    "base": f"myctmlib:blocks/machines/{name}/base",
                    "particle": f"myctmlib:blocks/machines/{name}/particles",
                },
            },
        )
        manifest["models"][name] = {"model": f"myctmlib:block/forestry/machines/{name}", "tanks": []}
        if resource:
            manifest["models"][name]["tanks"].append("resource")
        if product:
            manifest["models"][name]["tanks"].append("product")
        for tank, present in (("res", resource), ("prod", product)):
            for level in range(TANK_LEVELS):
                texture = texture_target / f"machines/{name}/tank_{tank}_{level}.png"
                if present and not texture.is_file():
                    raise SystemExit(f"missing tank texture: {texture}")

    escritoire = json.loads(ESCRITOIRE_MODEL.read_text(encoding="utf-8"))
    escritoire["textures"] = {
        "base": "myctmlib:blocks/escritoire",
        "particle": "myctmlib:blocks/escritoire.0",
    }
    escritoire.pop("groups", None)
    for element in escritoire["elements"]:
        for face in element["faces"].values():
            face["texture"] = {"#1": "#base"}.get(face.get("texture"), face.get("texture"))
    dump(models / "escritoire.json", escritoire)
    manifest["models"]["escritoire"] = {"model": "myctmlib:block/forestry/escritoire", "tanks": []}

    dump(output / "forestry/models.json", manifest)


if __name__ == "__main__":
    main()
