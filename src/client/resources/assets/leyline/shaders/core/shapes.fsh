#version 330

// Leyline's shapes, each drawn from its distance field: crisp, antialiased edges at any distance, and lines that never
// thin to nothing far away (they stay at least a pixel and a half wide, fading as they would have thinned).

#moj_import <leyline:shapes.glsl>

in vec2 local;
in vec2 spriteUv;
flat in int prim;

out vec4 fragColor;

const float TAU = 6.28318530718;

// How much of a pixel a stroke covers: {@code d} from its centre line, {@code halfWidth} wide each side, with
// {@code pixel} the size of a pixel in the same units.
float stroke(float d, float halfWidth, float pixel) {
    float drawn = max(halfWidth, pixel * 0.75);
    return clamp(0.5 - (d - drawn) / pixel, 0.0, 1.0) * (halfWidth / drawn);
}

uint hash(uint x) {
    x ^= x >> 16;
    x *= 0x7feb352du;
    x ^= x >> 15;
    x *= 0x846ca68bu;
    x ^= x >> 16;
    return x;
}

float segment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

vec2 rotate(vec2 p, float angle) {
    float c = cos(angle), s = sin(angle);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

// Distance to the outline of a regular polygon of {@code sides}, its corners {@code radius} from the centre.
float polygon(vec2 p, float sides, float radius) {
    float halfAngle = TAU * 0.5 / sides;
    float angle = mod(atan(p.x, p.y), 2.0 * halfAngle) - halfAngle;
    vec2 q = length(p) * vec2(cos(angle), abs(sin(angle)));
    q -= radius * vec2(cos(halfAngle), sin(halfAngle));
    q.y += clamp(-q.y, 0.0, radius * sin(halfAngle));
    return abs(length(q) * sign(q.x));
}

// Distance to an arc of the unit circle from angle {@code from} to {@code to}, with round ends.
float arc(vec2 p, float from, float to) {
    float sweep = to - from;
    if (sweep >= TAU) return abs(length(p) - 1.0);
    float t = mod(atan(p.y, p.x) - from, TAU);
    if (t <= sweep) return abs(length(p) - 1.0);
    vec2 start = vec2(cos(from), sin(from)), end = vec2(cos(to), sin(to));
    return min(length(p - start), length(p - end));
}

// One rune: three or four strokes between points of a three-by-three grid, chosen by its hash, sometimes with a ring.
float glyph(vec2 q, uint h) {
    float d = 1.0e3;
    int strokes = 3 + int(h & 1u);
    for (int i = 0; i < 4; i++) {
        if (i >= strokes) break;
        uint k = hash(h + uint(i) * 0x9e3779b9u);
        int s = int(k % 9u), e = int((k >> 8) % 9u);
        if (e == s) e = (s + 4) % 9;
        vec2 a = (vec2(float(s % 3), float(s / 3)) - 1.0) * 0.7;
        vec2 b = (vec2(float(e % 3), float(e / 3)) - 1.0) * 0.7;
        d = min(d, segment(q, a, b));
    }
    if (((h >> 12) & 3u) == 0u) d = min(d, abs(length(q) - 0.35));
    return d;
}

// Distance to a band of runes between radii {@code inner} and {@code outer}, turned by {@code turn}.
float runeBand(vec2 p, float inner, float outer, uint seed, float turn) {
    float halfBand = (outer - inner) * 0.5, middle = inner + halfBand;
    float r = length(p);
    if (abs(r - middle) > halfBand * 1.5) return 1.0e3;
    float count = max(6.0, floor(TAU * middle / (halfBand * 2.4)));
    float cell = TAU / count;
    float angle = atan(p.y, p.x) + turn;
    float index = floor(angle / cell);
    vec2 q = vec2((angle - (index + 0.5) * cell) * middle, r - middle) / halfBand;
    return glyph(q, hash(uint(mod(index, count)) + seed * 7919u)) * halfBand;
}

// How much of a pixel a rune circle covers: rings, bands of runes and stars, more of them for more layers.
float runeCircle(vec2 p, float line, float layers, uint seed, float turn, float pixel) {
    float r = length(p);
    float cover = stroke(abs(r - 1.0), line * 0.5, pixel);
    cover = max(cover, stroke(abs(r - 0.92), line * 0.3, pixel));
    cover = max(cover, stroke(runeBand(p, 0.75, 0.89, seed, turn), line * 0.3, pixel));
    cover = max(cover, stroke(abs(r - 0.72), line * 0.35, pixel));
    float sides = 5.0 + float(seed % 4u);
    if (layers >= 2.0) {
        cover = max(cover, stroke(polygon(rotate(p, -turn * 0.7), sides, 0.7), line * 0.3, pixel));
        cover = max(cover, stroke(abs(r - 0.06), line * 0.3, pixel));
    }
    if (layers >= 3.0) cover = max(cover, stroke(polygon(rotate(p, -turn * 0.7 + TAU * 0.5 / sides), sides, 0.7), line * 0.3, pixel));
    if (layers >= 4.0) {
        cover = max(cover, stroke(runeBand(p, 0.37, 0.47, seed + 1u, -turn * 1.5), line * 0.25, pixel));
        cover = max(cover, stroke(abs(r - 0.34), line * 0.3, pixel));
    }
    if (layers >= 5.0) {
        cover = max(cover, stroke(abs(r - 0.2), line * 0.3, pixel));
        cover = max(cover, stroke(polygon(rotate(p, turn * 2.0), 6.0, 0.18), line * 0.25, pixel));
    }
    if (layers >= 6.0) cover = max(cover, stroke(abs(r - 0.58), line * 0.2, pixel));
    return cover;
}

void main() {
    vec4 head = Prims[prim * 6];
    vec4 u = Prims[prim * 6 + 1];
    vec4 params = Prims[prim * 6 + 3];
    vec4 color = Prims[prim * 6 + 4];
    vec4 extra = Prims[prim * 6 + 5];
    int type = int(head.w + 0.5) % SHAPE_FACING;
    float pixel = max(max(fwidth(local.x), fwidth(local.y)), 1.0e-5);
    float r = length(local);
    float cover;

    if (type == SHAPE_DISC) {
        // A soft glow, brightest at the centre.
        cover = pow(clamp(1.0 - r, 0.0, 1.0), 1.6);
    } else if (type == SHAPE_RING) {
        cover = stroke(abs(r - 1.0), params.x * 0.5, pixel);
    } else if (type == SHAPE_ARC) {
        cover = stroke(arc(local, params.y, params.z), params.x * 0.5, pixel);
    } else if (type == SHAPE_POLYGON) {
        cover = stroke(polygon(rotate(local, -params.z), params.y, 1.0), params.x * 0.5, pixel);
    } else if (type == SHAPE_RUNES) {
        cover = runeCircle(local, params.x, params.y, uint(params.z), params.w, pixel);
    } else if (type == SHAPE_LINE) {
        // local: x along the line from its start, y across it, in blocks; the line's radius is u.w.
        float span = length(u.xyz);
        float off = length(vec2(local.x - clamp(local.x, 0.0, span), local.y));
        cover = params.x > 0.5
                ? pow(clamp(1.0 - off / (u.w * Prims[prim * 6 + 2].w), 0.0, 1.0), 2.0)
                : stroke(off, u.w, pixel);
    } else {
        // A beam without a sprite: bright down its middle, soft at its edges, fading at its ends.
        cover = pow(1.0 - abs(local.x), 2.0) * smoothstep(1.0, 0.85, abs(local.y));
    }
    if (cover <= 0.0) discard;
    fragColor = vec4(color.rgb * cover, color.a * cover * extra.x);
}
