#version 150

uniform sampler2D Sampler0;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = vec4(0.0);
    vec2 texel = vec2(1.0 / 1920.0, 1.0 / 1080.0);

    for (int x = -4; x <= 4; x++) {
        for (int y = -4; y <= 4; y++) {
            vec2 offset = texel * vec2(x, y);
            color += texture(Sampler0, texCoord + offset);
        }
    }

    fragColor = color / 81.0;
}
