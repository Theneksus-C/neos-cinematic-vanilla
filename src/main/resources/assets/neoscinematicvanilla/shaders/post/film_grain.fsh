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
};

out vec4 fragColor;

// Cheap hash noise. A texture lookup would be steadier but this costs nothing
// and grain wants to be different at every pixel anyway.
float hash(vec2 p) {
    p = fract(p * vec2(443.897, 441.423));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
}

void main() {
    vec4 color = texture(InSampler, texCoord);

    // Sampling in screen pixels rather than texture coordinates keeps a grain
    // the same apparent size at any resolution. Scaled by texture coordinates
    // it would be finer on a larger monitor, which is backwards.
    vec2 grainCoord = texCoord * ScreenSize;

    // GameTime advances every frame, so the pattern is reseeded continuously
    // rather than sitting still like dirt on the screen.
    float seed = GameTime * 8000.0;
    float noise = hash(grainCoord + vec2(seed, seed * 1.37));

    // Real film grain lives in the shadows. Weighting it by darkness keeps
    // bright sky clean and puts the texture where it belongs, which is also
    // what stops it reading as a filter laid over everything.
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    float shadowWeight = 1.0 - smoothstep(0.0, 0.85, luma);

    float grain = (noise - 0.5) * Strength * (0.3 + shadowWeight * 0.7);

    fragColor = vec4(clamp(color.rgb + grain, 0.0, 1.0), 1.0);
}
