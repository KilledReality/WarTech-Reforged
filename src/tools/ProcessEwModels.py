import os
import shutil
import sys

import bpy


ROOT = os.path.abspath(sys.argv[-1])
WORK = os.path.join(ROOT, "build", "ew_model_work")
MODEL_OUT = os.path.join(ROOT, "src", "resources", "assets", "wartecmod", "models", "ew")
TEXTURE_OUT = os.path.join(
    ROOT, "src", "resources", "assets", "wartecmod", "textures", "models", "ew"
)


def reset_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for data in (bpy.data.meshes, bpy.data.curves, bpy.data.materials, bpy.data.images):
        for value in list(data):
            data.remove(value)


def import_model(path):
    extension = os.path.splitext(path)[1].lower()
    if extension == ".fbx":
        bpy.ops.import_scene.fbx(filepath=path)
    elif extension == ".glb":
        bpy.ops.import_scene.gltf(filepath=path)
    elif extension == ".obj":
        bpy.ops.import_scene.obj(filepath=path)
    else:
        raise ValueError("Unsupported model format: " + extension)


def mesh_objects():
    return [value for value in bpy.context.scene.objects if value.type == "MESH"]


def delete_objects(names):
    for value in list(bpy.context.scene.objects):
        if value.name in names or value.type in {"CAMERA", "LIGHT"}:
            bpy.data.objects.remove(value, do_unlink=True)


def join_meshes():
    meshes = mesh_objects()
    if not meshes:
        raise RuntimeError("Imported scene contains no meshes")
    bpy.ops.object.select_all(action="DESELECT")
    for value in meshes:
        value.select_set(True)
    bpy.context.view_layer.objects.active = meshes[0]
    bpy.ops.object.join()
    result = bpy.context.object
    bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)
    return result


def normalize(model, target_height, target_length=None):
    corners = [model.matrix_world @ value.co for value in model.data.vertices]
    minimum = [min(value[index] for value in corners) for index in range(3)]
    maximum = [max(value[index] for value in corners) for index in range(3)]
    dimensions = [maximum[index] - minimum[index] for index in range(3)]
    scale = target_height / max(0.001, dimensions[2])
    if target_length is not None:
        scale = target_length / max(dimensions[0], dimensions[1], 0.001)
    model.scale = (scale, scale, scale)
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)

    corners = [model.matrix_world @ value.co for value in model.data.vertices]
    minimum = [min(value[index] for value in corners) for index in range(3)]
    maximum = [max(value[index] for value in corners) for index in range(3)]
    model.location.x -= (minimum[0] + maximum[0]) * 0.5
    model.location.y -= (minimum[1] + maximum[1]) * 0.5
    model.location.z -= minimum[2]
    bpy.ops.object.transform_apply(location=True, rotation=False, scale=False)


def material_atlas(model, colors, output_name):
    if not model.data.uv_layers:
        model.data.uv_layers.new(name="EWAtlas")
    uv_data = model.data.uv_layers.active.data
    material_count = max(1, len(model.data.materials))
    for polygon in model.data.polygons:
        color_index = polygon.material_index % len(colors)
        u = (color_index + 0.5) / len(colors)
        for loop_index in polygon.loop_indices:
            uv_data[loop_index].uv = (u, 0.5)

    width = len(colors) * 16
    image = bpy.data.images.new(output_name, width=width, height=16, alpha=True)
    pixels = []
    for _y in range(16):
        for x in range(width):
            color = colors[min(len(colors) - 1, x // 16)]
            pixels.extend((color[0], color[1], color[2], color[3]))
    image.pixels = pixels
    image.filepath_raw = os.path.join(TEXTURE_OUT, output_name + ".png")
    image.file_format = "PNG"
    image.save()
    print("atlas", output_name, "materials", material_count)


def export_obj(model, name):
    bpy.ops.object.select_all(action="DESELECT")
    model.select_set(True)
    bpy.context.view_layer.objects.active = model
    path = os.path.join(MODEL_OUT, name + ".obj")
    bpy.ops.export_scene.obj(
        filepath=path,
        use_selection=True,
        use_mesh_modifiers=True,
        use_edges=False,
        use_smooth_groups=False,
        use_normals=True,
        use_uvs=True,
        use_materials=False,
        use_triangles=True,
        group_by_object=False,
        group_by_material=False,
        keep_vertex_order=True,
        axis_forward="-Z",
        axis_up="Y",
    )
    triangles = sum(len(value.vertices) - 2 for value in model.data.polygons)
    print(name, "triangles", triangles, "vertices", len(model.data.vertices))


def process_flat(path, name, target_height, colors, remove_names=None):
    reset_scene()
    import_model(path)
    delete_objects(set(remove_names or []))
    model = join_meshes()
    normalize(model, target_height)
    material_atlas(model, colors, name)
    export_obj(model, name)


def process_textured(path, name, target_height, texture_source, texture_name,
                     target_length=None, texture_size=None):
    reset_scene()
    import_model(path)
    delete_objects(set())
    model = join_meshes()
    normalize(model, target_height, target_length)
    export_obj(model, name)
    texture_target = os.path.join(TEXTURE_OUT, texture_name)
    if texture_size is None:
        shutil.copyfile(texture_source, texture_target)
    else:
        image = bpy.data.images.load(texture_source, check_existing=False)
        image.scale(texture_size, texture_size)
        image.filepath_raw = texture_target
        image.file_format = "PNG"
        image.save()


def main():
    os.makedirs(MODEL_OUT, exist_ok=True)
    os.makedirs(TEXTURE_OUT, exist_ok=True)

    process_flat(
        os.path.join(WORK, "synytsia", "source", "EW.fbx"),
        "synytsia_jammer",
        3.2,
        [(0.075, 0.16, 0.055, 1.0), (0.42, 0.44, 0.40, 1.0)],
        set(),
    )
    process_flat(
        os.path.join(WORK, "bts", "source", "antenna1.glb"),
        "passive_esm_array",
        4.0,
        [(0.25, 0.27, 0.25, 1.0), (0.56, 0.58, 0.54, 1.0)],
        set(),
    )
    process_textured(
        os.path.join(WORK, "cc0", "source", "Anchor.fbx"),
        "radar_decoy",
        2.0,
        os.path.join(WORK, "cc0", "textures", "AntennaAlbedo.png"),
        "radar_decoy.png",
        texture_size=512,
    )
    process_textured(
        os.path.join(WORK, "agm88", "nested", "missiles_agm_88_harm.obj"),
        "agm88_harm",
        1.0,
        os.path.join(WORK, "agm88", "nested", "textures", "agm_88_harm.jpg"),
        "agm88_harm.png",
        target_length=2.8,
    )


if __name__ == "__main__":
    main()
