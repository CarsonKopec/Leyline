// Leyline's lights (see LightRenderer): what the vertex and fragment shaders share. Each light is three vec4s in Lights:
//   0: xyz position (camera-relative; a beam's start), w radius in blocks
//   1: rgb colour times brightness, w 0 for a point, 1 for a beam
//   2: xyz a beam's end

#define LIGHTS_PER_BATCH 128

layout(std140) uniform LightView {
    // Camera-relative world position to clip space, and back.
    mat4 ViewProj;
    mat4 InvViewProj;
    // xyz: the way the camera looks.
    vec4 Forward;
    // xy: the frame's size in pixels; z: how strongly lights light.
    vec4 Screen;
};

layout(std140) uniform Lights {
    vec4 Data[LIGHTS_PER_BATCH * 3];
};
