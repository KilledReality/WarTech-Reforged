import math
import pathlib
import re
import shutil
import zipfile

import bpy
from mathutils import Matrix, Vector


ROOT = pathlib.Path(__file__).resolve().parents[2]
WORK = ROOT / "build" / "tactical-aviation-import"
ASSETS = ROOT / "src" / "resources" / "assets" / "wartecmod"
MODEL_OUT = ASSETS / "models" / "tactical"
TEXTURE_OUT = ASSETS / "textures" / "models" / "tactical"


def reset_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for collection in (bpy.data.meshes, bpy.data.curves, bpy.data.materials,
                       bpy.data.images):
        for value in list(collection):
            collection.remove(value)


def extract_zip(source, destination):
    destination.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(str(source)) as archive:
        root = destination.resolve()
        for entry in archive.infolist():
            target = (destination / entry.filename).resolve()
            if root not in target.parents and target != root:
                raise RuntimeError("Unsafe archive entry: " + entry.filename)
        archive.extractall(str(destination))


def prepare_sources():
    f16 = WORK / "f16"
    su27 = WORK / "su27"
    weapons = WORK / "weapons"
    extract_zip(ROOT / "models_inbox" / "f16" / "f16-c-falcon.zip", f16)
    extract_zip(ROOT / "models_inbox" / "su27" / "sukhoi-su-27pu-ussr.zip", su27)
    nested = su27 / "source" / "su-27pu_ussr.zip"
    if nested.exists():
        extract_zip(nested, su27 / "source" / "model")
    extract_zip(ROOT / "models_inbox" / "missiles"
                / "missile-bomb-collection-fighter-jets-free.zip", weapons)
    return f16, su27, weapons


def meshes():
    return [value for value in bpy.context.scene.objects if value.type == "MESH"]


def sanitize(value):
    return re.sub(r"[^a-z0-9_]+", "_", value.lower()).strip("_")


def bake_world_transforms():
    for value in meshes():
        value.data = value.data.copy()
        world = value.matrix_world.copy()
        value.parent = None
        value.matrix_world = Matrix.Identity(4)
        value.data.transform(world)
        value.data.update()


def apply_to_all(matrix):
    for value in meshes():
        value.data.transform(matrix)
        value.data.update()


def normalize_group(target_length=1.0, ground=True):
    values = [value.co for obj in meshes() for value in obj.data.vertices]
    minimum = Vector(tuple(min(value[index] for value in values)
                           for index in range(3)))
    maximum = Vector(tuple(max(value[index] for value in values)
                           for index in range(3)))
    dimensions = maximum - minimum
    horizontal_length = max(dimensions.x, dimensions.y)
    scale = target_length / max(0.0001, horizontal_length)
    center = (minimum + maximum) * 0.5
    translation = Vector((-center.x, -center.y,
                          -minimum.z if ground else -center.z))
    transform = Matrix.Scale(scale, 4) @ Matrix.Translation(translation)
    apply_to_all(transform)


def export_obj(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.object.select_all(action="DESELECT")
    for value in meshes():
        value.select_set(True)
    bpy.ops.export_scene.obj(
        filepath=str(path),
        use_selection=True,
        use_materials=True,
        use_normals=True,
        use_uvs=True,
        use_triangles=True,
        use_mesh_modifiers=True,
        group_by_object=True,
        keep_vertex_order=True,
        axis_forward="-Z",
        axis_up="Y",
    )


def resize_texture(source, destination, maximum=1024):
    image = bpy.data.images.load(str(source), check_existing=False)
    width, height = image.size
    ratio = min(1.0, maximum / float(max(width, height)))
    if ratio < 1.0:
        image.scale(max(1, int(width * ratio)), max(1, int(height * ratio)))
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.filepath_raw = str(destination)
    image.file_format = "PNG"
    image.save()
    bpy.data.images.remove(image)


def solid_texture(destination, rgba, size=16):
    image = bpy.data.images.new(destination.stem, width=size, height=size,
                                alpha=True)
    image.pixels = list(rgba) * (size * size)
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.filepath_raw = str(destination)
    image.file_format = "PNG"
    image.save()
    bpy.data.images.remove(image)


def process_f16(source):
    reset_scene()
    bpy.ops.wm.open_mainfile(filepath=str(source / "source"
                                         / "Sketchfab_2022_02_28_12_38_46.blend"))
    for value in list(bpy.context.scene.objects):
        if value.type != "MESH":
            bpy.data.objects.remove(value, do_unlink=True)
            continue
        value.name = sanitize(value.name)
        value.data.name = value.name
    bake_world_transforms()
    # The source points toward Blender -Y. Canonical aircraft point +X.
    apply_to_all(Matrix.Rotation(math.radians(90.0), 4, "Z"))
    normalize_group(1.0, True)
    export_obj(MODEL_OUT / "f16_falcon.obj")
    resize_texture(source / "textures" / "F-16C_FightingFalcon_P01.png",
                   TEXTURE_OUT / "f16_body.png", 1024)
    resize_texture(source / "textures" / "Pilot_usa.png",
                   TEXTURE_OUT / "f16_pilot.png", 256)
    solid_texture(TEXTURE_OUT / "f16_seat.png", (0.17, 0.18, 0.16, 1.0))
    solid_texture(TEXTURE_OUT / "f16_glass.png", (0.08, 0.14, 0.17, 1.0))


def process_su27(source):
    reset_scene()
    model = source / "source" / "model" / "su-27pu_ussr.fbx"
    bpy.ops.import_scene.fbx(filepath=str(model))
    for value in list(bpy.context.scene.objects):
        if value.type != "MESH" or value.name == "Cube":
            bpy.data.objects.remove(value, do_unlink=True)
            continue
        value.name = {"mesh_369": "su27_body", "mesh_178": "su27_gear",
                      "mesh_370": "su27_glass"}.get(value.name,
                                                       sanitize(value.name))
        value.data.name = value.name
    bake_world_transforms()
    normalize_group(1.0, True)
    export_obj(MODEL_OUT / "su27_flanker.obj")
    resize_texture(source / "textures" / "su27pu-hull.png",
                   TEXTURE_OUT / "su27_body.png", 1024)
    resize_texture(source / "textures" / "su27pu-window.png",
                   TEXTURE_OUT / "su27_glass.png", 128)


WEAPONS = (
    ("AGM-114 Hellfire", "agm114_hellfire", "agm-114hellfire_d_dds.png"),
    ("GBU-12 Paveway II", "gbu12_paveway", "us_paveway_ii_d_dds.png"),
    ("HJ-10", "hj10", "ch_hj-10d_d.png"),
    ("AGM-65 Maverick", "agm65_maverick", "agm-65maverick_d_dds.png"),
    ("Kh-29", "kh29", "t_as-14kedge_dds.png"),
    ("KAB-500L", "kab500l", "kab-500l_d_dds.png"),
    ("JDAM", "jdam", "t_jdam_d_dds.png"),
)


def process_weapon(source_blend, texture_root, object_name, output_name,
                   texture_name):
    reset_scene()
    bpy.ops.wm.open_mainfile(filepath=str(source_blend))
    selected = bpy.data.objects.get(object_name)
    if selected is None or selected.type != "MESH":
        raise RuntimeError("Missing weapon mesh " + object_name)
    selected.data = selected.data.copy()
    for value in list(bpy.context.scene.objects):
        if value != selected:
            bpy.data.objects.remove(value, do_unlink=True)
    selected.name = output_name
    selected.data.name = output_name
    bake_world_transforms()
    # Collection weapons use their longitudinal axis as Z. After Minecraft's
    # OBJ axis conversion the extra Z rotation leaves the nose on model +X,
    # matching every aircraft renderer and the flight-vector convention.
    apply_to_all(Matrix.Rotation(math.radians(90.0), 4, "Y"))
    apply_to_all(Matrix.Rotation(math.radians(90.0), 4, "Z"))
    normalize_group(1.0, False)
    export_obj(MODEL_OUT / (output_name + ".obj"))
    resize_texture(texture_root / texture_name,
                   TEXTURE_OUT / (output_name + ".png"), 512)


MODEL_OUT.mkdir(parents=True, exist_ok=True)
TEXTURE_OUT.mkdir(parents=True, exist_ok=True)
f16_source, su27_source, weapon_source = prepare_sources()
process_f16(f16_source)
process_su27(su27_source)
for weapon in WEAPONS:
    process_weapon(weapon_source / "source" / "missiles.blend",
                   weapon_source / "textures", *weapon)
print("Tactical aviation assets written to", MODEL_OUT)
