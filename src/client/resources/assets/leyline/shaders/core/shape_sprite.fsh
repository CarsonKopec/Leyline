#version 330

// Leyline's beams drawn with a sprite: one frame of a flipbook laid side by side in the texture, tinted, adding light.

#moj_import <leyline:shapes.glsl>

uniform sampler2D Sprite;

in vec2 local;
in vec2 spriteUv;
flat in int prim;

out vec4 fragColor;

void main() {
    vec4 color = Prims[prim * 6 + 4];
    vec4 extra = Prims[prim * 6 + 5];
    vec4 texel = texture(Sprite, spriteUv);
    float cover = texel.a;
    if (cover <= 0.0) discard;
    fragColor = vec4(texel.rgb * color.rgb * cover, color.a * cover * extra.x);
}
