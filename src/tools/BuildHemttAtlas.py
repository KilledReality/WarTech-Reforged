from pathlib import Path
from PIL import Image, ImageEnhance


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "build" / "mobile-platform" / "converted" / "hemtt.obj"
TEXTURES = ROOT / "build" / "mobile-platform" / "model-source" / "textures"
MODEL_OUT = ROOT / "src" / "resources" / "assets" / "wartecmod" / "models" / "mobile" / "hemtt.obj"
ATLAS_OUT = ROOT / "src" / "resources" / "assets" / "wartecmod" / "textures" / "models" / "mobile" / "hemtt.png"

TILE = 256
GRID = 4

MATERIALS = [
    ("generic", "vehiclegeneric256.tga.png", (170, 178, 153)),
    ("primary", "vehiclegrunge256.tga.png", (91, 105, 60)),
    ("interior.001", "barracks92interior128.tga.png", (170, 170, 160)),
    ("primary.001", "vehiclegeneric256.tga.png", (91, 105, 60)),
    ("chassis_vlo.6", "vehiclelights128.tga.png", (160, 160, 145)),
    ("left front light", "vehiclelightson128.tga.png", (255, 230, 150)),
    ("right front light", "vehiclelightson128.tga.png", (255, 230, 150)),
    ("left rear light", "vehiclelightson128.tga.png", (255, 80, 60)),
    ("right rear light", "vehiclelightson128.tga.png", (255, 80, 60)),
    ("glass", "vehgenglass32.png", (75, 95, 100)),
    ("wheel.0", "vehicletyres128.tga.png", (150, 150, 145)),
    ("secondary", "vehiclegeneric256.tga.png", (70, 78, 48)),
    ("glass.002", "vehgenglass32.png", (75, 95, 100)),
    ("steering", "vehiclesteering128.tga.png", (145, 145, 135)),
]


def tint(image, color):
    image = image.convert("RGB").resize((TILE, TILE), Image.Resampling.LANCZOS)
    image = ImageEnhance.Contrast(image).enhance(1.08)
    overlay = Image.new("RGB", image.size, color)
    return Image.blend(image, overlay, 0.34)


def build_atlas():
    atlas = Image.new("RGB", (TILE * GRID, TILE * GRID), (64, 70, 52))
    slots = {}
    for index, (name, filename, color) in enumerate(MATERIALS):
        x = (index % GRID) * TILE
        y = (index // GRID) * TILE
        atlas.paste(tint(Image.open(TEXTURES / filename), color), (x, y))
        slots[name] = (index % GRID, index // GRID)
    ATLAS_OUT.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(ATLAS_OUT, optimize=True)
    return slots


def build_obj(slots):
    lines = SOURCE.read_text(encoding="utf-8").splitlines()
    texcoords = [None]
    static = []
    faces = []
    material = "generic"

    for line in lines:
        if line.startswith("vt "):
            _, u, v = line.split()[:3]
            texcoords.append((float(u), float(v)))
        elif line.startswith("usemtl "):
            material = line[7:]
        elif line.startswith("f "):
            faces.append((material, line[2:].split()))
        elif not line.startswith("mtllib ") and not line.startswith("usemtl "):
            static.append(line)

    output = ["# Optimized for WarTech Reforged; source credit in MODEL_CREDITS.txt"]
    output.extend(line for line in static if not line.startswith("vt "))
    rewritten_faces = []
    next_vt = 1
    for face_material, vertices in faces:
        col, row = slots.get(face_material, slots["generic"])
        rewritten = []
        for vertex in vertices:
            parts = vertex.split("/")
            original = int(parts[1]) if len(parts) > 1 and parts[1] else 0
            u, v = texcoords[original] if original else (0.5, 0.5)
            u = (col + u) / GRID
            v = (GRID - 1 - row + v) / GRID
            output.append("vt %.8f %.8f" % (u, v))
            normal = parts[2] if len(parts) > 2 else ""
            rewritten.append("%s/%d/%s" % (parts[0], next_vt, normal))
            next_vt += 1
        rewritten_faces.append("f " + " ".join(rewritten))
    output.extend(rewritten_faces)
    MODEL_OUT.parent.mkdir(parents=True, exist_ok=True)
    MODEL_OUT.write_text("\n".join(output) + "\n", encoding="ascii")


if __name__ == "__main__":
    build_obj(build_atlas())
