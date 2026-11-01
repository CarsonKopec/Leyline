#version 330

// Leyline's shapes: six vertices per shape, two triangles, placed from the shape's data alone (no vertex buffer).

#moj_import <leyline:shapes.glsl>

out vec2 local;
out vec2 spriteUv;
flat out int prim;

const vec2 CORNERS[6] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(1, 1), vec2(-1, -1), vec2(1, 1), vec2(-1, 1));

// A unit vector at right angles to both a and b, or to a alone if they line up.
vec3 across(vec3 a, vec3 b) {
    vec3 c = cross(a, b);
    if (dot(c, c) < 1.0e-8) c = cross(a, abs(a.y) < 0.99 ? vec3(0, 1, 0) : vec3(1, 0, 0));
    return normalize(c);
}

void main() {
    prim = gl_VertexID / 6;
    vec2 corner = CORNERS[gl_VertexID % 6];
    vec4 head = Prims[prim * 6];
    vec4 u = Prims[prim * 6 + 1];
    vec4 v = Prims[prim * 6 + 2];
    int type = int(head.w + 0.5);
    bool facing = type >= SHAPE_FACING;
    type = type % SHAPE_FACING;
    vec3 center = head.xyz;
    vec3 position;
    spriteUv = vec2(0);

    if (type == SHAPE_LINE) {
        // A capsule from the start to the end, on a strip turned to face the camera.
        float span = max(length(u.xyz), 1.0e-4);
        vec3 along = u.xyz / span;
        vec3 middle = center + u.xyz * 0.5;
        vec3 side = across(along, -middle);
        // At least two pixels across, so a thin line far off still covers pixels to fade into.
        float reach = max(u.w * v.w, 2.0 * pixelAt(middle));
        float halfSpan = span * 0.5 + reach;
        position = middle + along * corner.x * halfSpan + side * corner.y * reach;
        local = vec2(corner.x * halfSpan + span * 0.5, corner.y * reach);
    } else if (type == SHAPE_BEAM) {
        // A strip along the beam's axis, turned about it to face the camera.
        vec3 axis = normalize(u.xyz);
        vec3 side = across(axis, -center);
        position = center + u.xyz * corner.y + side * max(u.w, pixelAt(center)) * corner.x;
        local = corner;
        vec4 extra = Prims[prim * 6 + 5];
        float frames = max(extra.z, 1.0);
        spriteUv = vec2((extra.y + corner.x * 0.5 + 0.5) / frames, 0.5 - corner.y * 0.5);
    } else {
        vec3 axisU = u.xyz, axisV = v.xyz;
        if (facing) {
            vec3 toCamera = normalize(-center);
            axisU = across(vec3(0, 1, 0), toCamera);
            axisV = cross(toCamera, axisU);
        }
        // Two pixels past the shape's own reach, for lines that keep a pixel's width far off.
        float extent = v.w + 2.0 * pixelAt(center) / max(u.w, 1.0e-4);
        position = center + (axisU * corner.x + axisV * corner.y) * (u.w * extent);
        local = corner * extent;
    }
    gl_Position = ViewProj * vec4(position, 1.0);
}
