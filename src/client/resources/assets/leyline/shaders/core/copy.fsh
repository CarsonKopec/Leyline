#version 330

// Copies the frame for a screen pass to read while it draws over it (see ScreenPasses.copy).

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = vec4(texture(InSampler, texCoord).rgb, 1.0);
}
