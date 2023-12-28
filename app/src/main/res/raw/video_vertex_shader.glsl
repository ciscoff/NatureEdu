#version 300 es
uniform mat4 u_MvpMatrix;
uniform mat4 u_StMatrix;

in vec3 a_Position;
in vec2 a_TexPos;

out vec2 v_TextPos;

void main() {
    v_TextPos = (u_StMatrix * vec4(a_TexPos.s, a_TexPos.t, 0.0, 1.0)).st;

    //v_TextPos = a_TexPos;
    gl_Position = u_MvpMatrix * vec4(a_Position, 1.0);
}