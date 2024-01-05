#version 300 es
uniform mat4 u_MvpMatrix;
uniform mat4 u_StMatrix;

in vec4 a_Position;
in vec4 a_TexPos;

out vec2 v_TextPos;

void main() {
    v_TextPos = (u_StMatrix * a_TexPos).xy;
    gl_Position = u_MvpMatrix * a_Position;
}