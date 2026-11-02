#version 330

// Leyline's lights: six vertices per light, a quad over just the part of the screen its light can reach, or the whole
// screen when the camera is within its reach.

#moj_import <leyline:lights.glsl>

flat out int light;

const vec2 CORNERS[6] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(1, 1), vec2(-1, -1), vec2(1, 1), vec2(-1, 1));

void main() {
    light = gl_VertexID / 6;
    vec2 corner = CORNERS[gl_VertexID % 6];
    vec4 start = Data[light * 3];
    vec4 color = Data[light * 3 + 1];
    vec3 center = start.xyz;
    float reach = start.w;
    if (color.w > 0.5) {
        vec3 end = Data[light * 3 + 2].xyz;
        center = (start.xyz + end) * 0.5;
        reach += length(end - start.xyz) * 0.5;
    }
    float d = length(center);
    if (d < reach * 1.05 || dot(center, Forward.xyz) < reach) {
        gl_Position = vec4(corner, 0.0, 1.0);
        return;
    }
    // A quad facing the camera, as wide as the cone from the camera that just touches the light's sphere.
    vec3 toCamera = -center / d;
    vec3 right = cross(vec3(0, 1, 0), toCamera);
    right = dot(right, right) < 1.0e-6 ? vec3(1, 0, 0) : normalize(right);
    vec3 up = cross(toCamera, right);
    float size = reach * d / sqrt(d * d - reach * reach);
    gl_Position = ViewProj * vec4(center + (right * corner.x + up * corner.y) * size, 1.0);
}
