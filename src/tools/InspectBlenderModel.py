import pathlib
import sys

import bpy
from mathutils import Vector


def argument(name):
    args = sys.argv[sys.argv.index("--") + 1:]
    index = args.index(name)
    return args[index + 1]


path = pathlib.Path(argument("--input")).resolve()
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)

extension = path.suffix.lower()
if extension == ".fbx":
    bpy.ops.import_scene.fbx(filepath=str(path))
elif extension in (".gltf", ".glb"):
    bpy.ops.import_scene.gltf(filepath=str(path))
elif extension == ".dae":
    bpy.ops.wm.collada_import(filepath=str(path))
else:
    raise RuntimeError("Unsupported model format: " + extension)

print("MODEL", path)
for obj in sorted(bpy.context.scene.objects, key=lambda value: value.name):
    if obj.type != "MESH":
        print("OBJECT", obj.name, obj.type)
        continue
    polygons = len(obj.data.polygons)
    triangles = sum(max(1, len(poly.vertices) - 2) for poly in obj.data.polygons)
    materials = [slot.material.name if slot.material else "<none>" for slot in obj.material_slots]
    corners = [obj.matrix_world @ Vector(corner) for corner in obj.bound_box]
    mins = tuple(round(min(value[index] for value in corners), 4) for index in range(3))
    maxs = tuple(round(max(value[index] for value in corners), 4) for index in range(3))
    print("MESH", obj.name, "polygons=", polygons, "triangles=", triangles,
          "materials=", materials, "bounds=", mins, maxs)
