#version 330 core

layout (location = 0) in vec4 pos;
layout (location = 1) in vec2 texCoords;
layout (location = 2) in vec4 color;
out vec2 v_TexCoord;
out vec4 v_Color;

uniform mat4 projection;
uniform mat4 modelView;

void main() {
    gl_Position = projection * modelView * pos;

    v_TexCoord = texCoords;
    v_Color = color;
}