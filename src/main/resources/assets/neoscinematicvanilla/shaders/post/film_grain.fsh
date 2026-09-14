#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform GrainConfig {
    float Strength;
    float GrainSize;
};

out vec4 fragColor;

// Hash without sine, after Dave Hoskins.
//
// Inputs are reduced into the unit range on the first line, before any large
// multiply, which is the whole point of it. An earlier version multiplied raw
// pixel coordinates by several hundred first, reaching values near a million
// where 32 bit float precision starts dropping low bits. Losing those bits made
// neighbouring pixels resolve to the same value in runs, which reads as regular
// banding rather than as noise.
float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec4 color = texture(InSampler, texCoord);

    // Sampling in pixels of the source texture keeps a grain the same apparent
    // size at any resolution. InSize is the sampler's own dimensions, which is
    // what this pass is actually reading, unlike the window size.
    vec2 pixelCoord = texCoord * InSize;

    // Dividing before hashing clumps several pixels into one grain. Per pixel
    // noise is video static; film grain is made of clumps large enough to see.
    vec2 grainCoord = pixelCoord / max(GrainSize, 0.5);

    // Time enters as a third dimension rather than as an offset added to the
    // coordinates. Offsetting slides one pattern across the screen, which the
    // eye follows as drifting structure.
    float seed = fract(GameTime * 1024.0) * 137.0;

    // Two octaves at unrelated scales, so neither one's residual regularity
    // survives in the sum.
    float coarse = hash13(vec3(grainCoord, seed));
    float fine = hash13(vec3(grainCoord * 2.17 + 11.3, seed * 1.73 + 4.1));

    float noise = coarse * 0.62 + fine * 0.38;

    // Real film grain lives in the shadows. Weighting it by darkness keeps
    // bright sky clean and puts the texture where it belongs, which is also
    // what stops it reading as a filter laid over everything.
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    float shadowWeight = 1.0 - smoothstep(0.0, 0.85, luma);

    float grain = (noise - 0.5) * Strength * (0.3 + shadowWeight * 0.7);

    fragColor = vec4(clamp(color.rgb + grain, 0.0, 1.0), 1.0);
}
