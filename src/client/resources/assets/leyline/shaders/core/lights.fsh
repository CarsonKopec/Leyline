#version 330

// One light on the finished frame. Each pixel's position comes back from the depth it was drawn at, and its surface's
// facing from its neighbours'; the light adds its colour times the surface's own, fading smoothly to nothing at its
// radius, most on the surfaces turned towards it. There's no dithering: the light is smooth. A strong light eases off
// as it grows, so it brightens a surface a long way without flattening it to white.

#moj_import <leyline:lights.glsl>

uniform sampler2D SceneDepth;
uniform sampler2D SceneColor;

flat in int light;

out vec4 fragColor;

// The most light can add: this many times the surface's own colour.
const float MAX_LIGHT = 1.4;

vec3 positionAt(ivec2 pixel) {
    float depth = texelFetch(SceneDepth, pixel, 0).r;
    vec2 ndc = (vec2(pixel) + 0.5) / Screen.xy * 2.0 - 1.0;
    vec4 p = InvViewProj * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

// Of the two steps to a pixel's neighbours, the shorter: across an edge, the far one would bend the normal.
vec3 nearer(vec3 forward, vec3 back) {
    return dot(forward, forward) < dot(back, back) ? forward : back;
}

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    if (texelFetch(SceneDepth, pixel, 0).r >= 1.0) discard;
    vec3 p = positionAt(pixel);

    vec4 start = Data[light * 3];
    vec4 color = Data[light * 3 + 1];
    vec3 source = start.xyz;
    if (color.w > 0.5) {
        vec3 along = Data[light * 3 + 2].xyz - start.xyz;
        source += along * clamp(dot(p - start.xyz, along) / max(dot(along, along), 1.0e-6), 0.0, 1.0);
    }
    vec3 toLight = source - p;
    float distance2 = dot(toLight, toLight);
    float reach = start.w;
    if (distance2 >= reach * reach) discard;
    float x2 = distance2 / (reach * reach);
    float falloff = (1.0 - x2) * (1.0 - x2);

    ivec2 last = ivec2(Screen.xy) - 1;
    vec3 dx = nearer(positionAt(min(pixel + ivec2(1, 0), last)) - p, p - positionAt(max(pixel - ivec2(1, 0), ivec2(0))));
    vec3 dy = nearer(positionAt(min(pixel + ivec2(0, 1), last)) - p, p - positionAt(max(pixel - ivec2(0, 1), ivec2(0))));
    vec3 normal = cross(dx, dy);
    float facing = 1.0;
    if (dot(normal, normal) > 1.0e-12 && distance2 > 1.0e-6) {
        normal = normalize(normal);
        if (dot(normal, -p) < 0.0) normal = -normal;
        facing = clamp(dot(normal, toLight * inversesqrt(distance2)), 0.0, 1.0) * 0.8 + 0.2;
    }

    // The surface's own colour, roughly: what the frame shows, lifted where it's dark, so a light shows a dark wall's colour.
    vec3 frame = texelFetch(SceneColor, pixel, 0).rgb;
    float luma = dot(frame, vec3(0.2126, 0.7152, 0.0722));
    vec3 surface = frame * (0.65 + 0.35 / (luma + 0.1));
    vec3 lit = color.rgb * (falloff * facing * Screen.z);
    lit = MAX_LIGHT * (1.0 - exp(-lit / MAX_LIGHT));
    fragColor = vec4(surface * lit, 0.0);
}
