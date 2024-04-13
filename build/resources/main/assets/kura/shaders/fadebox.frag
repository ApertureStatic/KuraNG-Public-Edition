#version 330 core

uniform vec3 minBox;
uniform vec3 maxBox;
uniform vec4 color1;
uniform vec4 color2;
uniform mat4 projection;
uniform mat4 modelView;

in vec3 position;
out vec4 fragColor;

void main() {
    // Define the box dimensions in normalized device coordinates (NDC)

    // Get fragment position in NDC
    vec4 fragPos = projection * modelView * vec4(position, 1.0);

    // Apply fade based on fragment position
    float fade = smoothstep(minBox.x, minBox.x + 0.1, fragPos.x) *
    smoothstep(minBox.y, minBox.y + 0.1, fragPos.y) *
    smoothstep(minBox.z, minBox.z + 0.1, fragPos.z) *
    smoothstep(maxBox.x - 0.1, maxBox.x, fragPos.x) *
    smoothstep(maxBox.y - 0.1, maxBox.y, fragPos.y) *
    smoothstep(maxBox.z - 0.1, maxBox.z, fragPos.z);

    // Interpolate colors based on fade
    vec4 finalColor = mix(color1, color2, fade);

    // Output the final color
    fragColor = finalColor;
}
