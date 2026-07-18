import pathlib
import re
import shutil
import sys
import math

import bpy
from mathutils import Matrix


ROOT = pathlib.Path(__file__).resolve().parents[2]
WORK = ROOT / "build" / "mq9-import"
OUTPUT = ROOT / "src" / "resources" / "assets" / "wartecmod"
MODEL_OUTPUT = OUTPUT / "models" / "mq9"
TEXTURE_OUTPUT = OUTPUT / "textures" / "models" / "mq9"


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)


def import_model(path):
    extension = path.suffix.lower()
    if extension == ".fbx":
        bpy.ops.import_scene.fbx(filepath=str(path))
    elif extension in (".gltf", ".glb"):
        bpy.ops.import_scene.gltf(filepath=str(path))
    elif extension == ".dae":
        bpy.ops.wm.collada_import(filepath=str(path))
    else:
        raise RuntimeError("Unsupported model format: " + extension)


def mesh_objects():
    return [obj for obj in bpy.context.scene.objects if obj.type == "MESH"]


def sanitize(value):
    value = value.lower().replace("g ", "")
    return re.sub(r"[^a-z0-9_]+", "_", value).strip("_")


def decimate(obj, ratio):
    if ratio >= 0.999 or len(obj.data.polygons) < 200:
        return
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    modifier = obj.modifiers.new(name="wartech_lod", type="DECIMATE")
    modifier.ratio = ratio
    modifier.use_collapse_triangulate = True
    bpy.ops.object.modifier_apply(modifier=modifier.name)
    obj.select_set(False)


def export_obj(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in mesh_objects():
        obj.select_set(True)
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


def resize_texture(source, destination, maximum=512):
    image = bpy.data.images.load(str(source), check_existing=False)
    width, height = image.size
    scale = min(1.0, maximum / float(max(width, height)))
    if scale < 1.0:
        image.scale(max(1, int(width * scale)), max(1, int(height * scale)))
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.filepath_raw = str(destination)
    image.file_format = "PNG"
    image.save()
    bpy.data.images.remove(image)


def solid_texture(destination, rgba):
    image = bpy.data.images.new(destination.stem, width=16, height=16, alpha=True)
    image.pixels = list(rgba) * (16 * 16)
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.filepath_raw = str(destination)
    image.file_format = "PNG"
    image.save()
    bpy.data.images.remove(image)


def process_airframe():
    clear_scene()
    source = WORK / "airframe" / "source" / "unpacked" / "model" / "model.dae"
    import_model(source)
    for obj in list(mesh_objects()):
        name = sanitize(obj.name)
        if "missile_" in name or name.startswith("big_rocket_left") \
                or name.startswith("big_rocket_right"):
            bpy.data.objects.remove(obj, do_unlink=True)
            continue
        obj.name = name
        obj.data.name = name
        if "camera" in name:
            decimate(obj, 0.48)
        elif "rocket_system" in name:
            decimate(obj, 0.34)
        elif "propeller" in name:
            decimate(obj, 0.62)
    export_obj(MODEL_OUTPUT / "mq9_reaper.obj")

    textures = source.parent / "textures"
    mapping = {
        "Body_mat_albedo.jpg": "mq9_body.png",
        "Wing_mat_albedo.jpg": "mq9_wing.png",
        "CameraSystem_mat_albedo.jpg": "mq9_camera.png",
        "Extras_mat_albedo.jpg": "mq9_extras.png",
        "Rocket_system_mat_albedo.jpg": "mq9_pylons.png",
    }
    for original, converted in mapping.items():
        resize_texture(textures / original, TEXTURE_OUTPUT / converted, 512)


def canonicalize_ordnance(obj, rotation_axis=None, rotation_degrees=0.0):
    # Imported Collada/FBX files often keep their unit conversion on an Empty.
    # Bake the complete world transform before detaching so every payload uses
    # the same one-unit coordinate system in the runtime renderer.
    obj.data.transform(obj.matrix_world)
    obj.parent = None
    obj.matrix_world = Matrix.Identity(4)
    if rotation_axis == "z":
        obj.rotation_euler[2] = math.radians(rotation_degrees)
    elif rotation_axis == "y":
        obj.rotation_euler[1] = math.radians(rotation_degrees)
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)

    # OBJ export uses X=Blender X, Y=Blender Z, Z=-Blender Y.
    xs = [vertex.co.x for vertex in obj.data.vertices]
    ys = [vertex.co.y for vertex in obj.data.vertices]
    zs = [vertex.co.z for vertex in obj.data.vertices]
    center_x = (min(xs) + max(xs)) * 0.5
    center_y = (min(ys) + max(ys)) * 0.5
    top_z = max(zs)
    for vertex in obj.data.vertices:
        vertex.co.x -= center_x
        vertex.co.y -= center_y
        vertex.co.z -= top_z
    material = bpy.data.materials.new("wartech_material")
    material.diffuse_color = (1.0, 1.0, 1.0, 1.0)
    obj.data.materials.clear()
    obj.data.materials.append(material)
    for polygon in obj.data.polygons:
        polygon.material_index = 0


def join_and_export(source, output_name, ratio=1.0,
                    rotation_axis=None, rotation_degrees=0.0):
    clear_scene()
    import_model(source)
    objects = mesh_objects()
    bpy.ops.object.select_all(action="DESELECT")
    for obj in objects:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = objects[0]
    bpy.ops.object.join()
    joined = bpy.context.active_object
    joined.name = output_name
    joined.data.name = output_name
    decimate(joined, ratio)
    canonicalize_ordnance(joined, rotation_axis, rotation_degrees)
    export_obj(MODEL_OUTPUT / (output_name + ".obj"))


def process_ordnance():
    join_and_export(
        WORK / "hellfire" / "source" / "Hellfire_missile.fbx",
        "agm114_hellfire",
        rotation_axis="y",
        rotation_degrees=90.0,
    )
    join_and_export(
        WORK / "gbu12" / "source" / "unpacked" / "GBU 12.dae",
        "gbu12_paveway",
        0.19,
        rotation_axis="z",
        rotation_degrees=-90.0,
    )
    join_and_export(
        WORK / "mk82" / "source" / "model (2).gltf",
        "mk82_bomb",
    )
    resize_texture(
        WORK / "hellfire" / "textures" / "Hellfire_missile_DefaultMaterial_BaseColor.png",
        TEXTURE_OUTPUT / "agm114_hellfire.png",
        256,
    )
    solid_texture(TEXTURE_OUTPUT / "gbu12_paveway.png", (0.30, 0.34, 0.23, 1.0))
    solid_texture(TEXTURE_OUTPUT / "mk82_bomb.png", (0.25, 0.27, 0.20, 1.0))


MODEL_OUTPUT.mkdir(parents=True, exist_ok=True)
TEXTURE_OUTPUT.mkdir(parents=True, exist_ok=True)
process_airframe()
process_ordnance()
print("MQ-9 models written to", MODEL_OUTPUT)
