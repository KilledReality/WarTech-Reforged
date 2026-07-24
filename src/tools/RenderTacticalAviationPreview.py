import math
import pathlib

import bpy
from mathutils import Vector


ROOT = pathlib.Path(__file__).resolve().parents[2]
MODELS = ROOT / "src/resources/assets/wartecmod/models/tactical"
TEXTURES = ROOT / "src/resources/assets/wartecmod/textures/models/tactical"
OUTPUT = ROOT / "build/tactical-aviation-preview.png"


def clear():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)


def material(name, texture):
    value = bpy.data.materials.new(name)
    value.diffuse_color = (0.45, 0.48, 0.47, 1.0)
    value.use_nodes = True
    node = value.node_tree.nodes.get("Principled BSDF")
    image = value.node_tree.nodes.new("ShaderNodeTexImage")
    image.image = bpy.data.images.load(str(TEXTURES / texture))
    value.node_tree.links.new(image.outputs["Color"], node.inputs["Base Color"])
    return value


def import_model(name, scale, location):
    before = set(bpy.context.scene.objects)
    bpy.ops.import_scene.obj(filepath=str(MODELS / name), use_split_objects=True)
    values = [value for value in bpy.context.scene.objects if value not in before]
    for value in values:
        value.scale = (scale, scale, scale)
        value.location = location
    return values


def assign(values, fallback, rules=()):
    for value in values:
        selected = fallback
        for token, candidate in rules:
            if token in value.name.lower():
                selected = candidate
                break
        value.data.materials.clear()
        value.data.materials.append(selected)


def weapon(name, texture, scale, location, yaw=0.0):
    values = import_model(name + ".obj", scale, location)
    for value in values:
        value.rotation_euler[1] = math.radians(yaw)
    assign(values, material(name, texture))


WEAPON_TOP = {
    "agm65_maverick": 0.102,
    "kh29": 0.100,
    "kab500l": 0.079,
    "hj10": 0.073,
    "jdam": 0.068,
}


def mount_height(name, scale, underside):
    return underside - WEAPON_TOP[name] * scale - 0.035


def point_camera(camera, target):
    camera.rotation_euler = (Vector(target) - camera.location).to_track_quat(
            "-Z", "Y").to_euler()


def scene_bounds():
    points = []
    for value in bpy.context.scene.objects:
        if value.type != "MESH" or value.name == "Plane":
            continue
        points.extend(value.matrix_world @ Vector(corner)
                      for corner in value.bound_box)
    minimum = Vector(tuple(min(point[index] for point in points)
                           for index in range(3)))
    maximum = Vector(tuple(max(point[index] for point in points)
                           for index in range(3)))
    return (minimum + maximum) * 0.5, maximum - minimum


clear()
f16_body = material("f16_body", "f16_body.png")
f16_pilot = material("f16_pilot", "f16_pilot.png")
f16_glass = material("f16_glass", "f16_glass.png")
f16 = import_model("f16_falcon.obj", 10.5, (0.0, 0.0, -5.2))
assign(f16, f16_body, (("pilot", f16_pilot), ("glass", f16_glass)))

su_body = material("su27_body", "su27_body.png")
su_glass = material("su27_glass", "su27_glass.png")
su27 = import_model("su27_flanker.obj", 12.5, (0.0, 0.0, 5.2))
assign(su27, su_body, (("glass", su_glass),))

f16_loads = (("agm65_maverick", "agm65_maverick.png", 1.55),
             ("jdam", "jdam.png", 1.55),
             ("jdam", "jdam.png", 1.55),
             ("agm65_maverick", "agm65_maverick.png", 1.55))
for model_x, underside, offset, load in zip(
        (1.55, 0.82, 0.82, 1.55),
        (0.72, 0.90, 0.90, 0.72),
        (-2.18, -1.08, 1.08, 2.18), f16_loads):
    scale = load[2] * 0.92
    weapon(load[0], load[1], scale,
           (model_x, mount_height(load[0], scale, underside), -5.2 + offset))

su_loads = (("kh29", "kh29.png", 1.85),
            ("kab500l", "kab500l.png", 1.60),
            ("hj10", "hj10.png", 1.30),
            ("hj10", "hj10.png", 1.30),
            ("kab500l", "kab500l.png", 1.60),
            ("kh29", "kh29.png", 1.85))
for model_x, underside, offset, load in zip(
        (-3.20, -1.62, -0.55, -0.55, -1.62, -3.20),
        (1.40, 1.07, 0.96, 0.96, 1.07, 1.40),
        (-3.00, -2.00, -1.00, 1.00, 2.00, 3.00), su_loads):
    scale = load[2] * 0.92
    weapon(load[0], load[1], scale,
           (model_x, mount_height(load[0], scale, underside), 5.2 + offset),
           180.0)

bpy.ops.mesh.primitive_plane_add(size=40, location=(0.0, -0.06, 0.0))
ground = bpy.context.object
ground.data.materials.append(bpy.data.materials.new("ground"))
ground.data.materials[0].diffuse_color = (0.09, 0.12, 0.10, 1.0)

bpy.ops.object.light_add(type="AREA", location=(2.0, 14.0, -4.0))
bpy.context.object.data.energy = 1900
bpy.context.object.data.size = 12
bpy.ops.object.light_add(type="AREA", location=(-8.0, 7.0, 10.0))
bpy.context.object.data.energy = 1000
bpy.context.object.data.size = 9
center, dimensions = scene_bounds()
bpy.ops.object.camera_add(location=center + Vector((18.0, 18.0, 22.0)))
camera = bpy.context.object
point_camera(camera, center)
camera.data.type = "ORTHO"
camera.data.ortho_scale = max(dimensions.x, dimensions.z) * 1.35
bpy.context.scene.camera = camera
bpy.context.scene.render.engine = "BLENDER_EEVEE"
bpy.context.scene.render.resolution_x = 1280
bpy.context.scene.render.resolution_y = 800
bpy.context.scene.render.resolution_percentage = 100
bpy.context.scene.render.image_settings.file_format = "PNG"
bpy.context.scene.render.filepath = str(OUTPUT)
bpy.context.scene.world.color = (0.025, 0.035, 0.05)
bpy.ops.render.render(write_still=True)
print("Preview written to", OUTPUT)
