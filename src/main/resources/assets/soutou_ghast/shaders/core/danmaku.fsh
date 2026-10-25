#version 150
uniform sampler2D Sampler0;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    if (texCoord0.x < 0.0) {
        float across = max(0.0, 1.0 - abs(texCoord0.x + 1.5) * 2.0);
        float along = clamp(texCoord0.y, 0.0, 1.0);
        float alpha = vertexColor.a * across * across * along * along;
        if (alpha < 0.001) discard;
        fragColor = vec4(vertexColor.rgb, alpha);
        return;
    }
    bool halo = texCoord0.x >= 2.0;
    vec2 local = vec2(halo ? texCoord0.x - 2.0 : texCoord0.x, texCoord0.y);
    float distanceFromCenter = length(local * 2.0 - 1.0);
    float shape = 1.0 - smoothstep(0.92, 1.0, distanceFromCenter);
    if (shape < 0.001) discard;
    // Crop UVs, not source pixels: all four original RGB textures remain byte-identical.
    vec4 source = texture(Sampler0, vec2(0.347) + local * 0.306);
    if (halo) {
        float brightness = max(source.r, max(source.g, source.b));
        float alpha = vertexColor.a * shape * brightness;
        if (alpha < 0.001) discard;
        fragColor = vec4(source.rgb * vertexColor.rgb, alpha);
    } else {
        // Preserve intentional black cores/interiors; only outside-circle geometry is transparent.
        fragColor = vec4(source.rgb * vertexColor.rgb, vertexColor.a * shape);
    }
}
