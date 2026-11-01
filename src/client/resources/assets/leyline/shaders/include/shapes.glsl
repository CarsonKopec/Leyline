// Leyline's shapes (see ShapeRenderer): what the vertex and fragment shaders share. Each shape is six vec4s in Shapes:
//   0: xyz centre (camera-relative; a line's start), w type (+16 to face the camera)
//   1: xyz first in-plane axis (a line's start-to-end, a beam's half axis), w size in blocks (a line's or beam's radius)
//   2: xyz second in-plane axis, w how far the quad reaches past the shape, in sizes
//   3: the type's parameters
//   4: colour, premultiplied
//   5: x how much it covers what's behind it, 0 to 1; y the beam's frame; z the beam's frames

#define SHAPE_DISC 0
#define SHAPE_RING 1
#define SHAPE_ARC 2
#define SHAPE_POLYGON 3
#define SHAPE_RUNES 4
#define SHAPE_LINE 5
#define SHAPE_BEAM 6
#define SHAPE_FACING 16

#define SHAPES_PER_BATCH 160

layout(std140) uniform ShapeView {
    // Camera-relative world position to clip space: the projection and the camera's rotation, as the world is drawn.
    mat4 ViewProj;
    // x: pixels a block spans one block from the camera; a block d away spans x / d.
    vec4 PixelScale;
};

// How many blocks a pixel spans at {@code p}, camera-relative.
float pixelAt(vec3 p) {
    return length(p) / max(PixelScale.x, 1.0e-3);
}

layout(std140) uniform Shapes {
    vec4 Prims[SHAPES_PER_BATCH * 6];
};
