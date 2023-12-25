#version 300 es
uniform mat4 u_ViewMatrix;
uniform mat4 u_ProjMatrix;
uniform mat4 u_ModelMatrix;

in vec3 a_Position;
in vec3 a_Normal;

out vec3 v_FragPos;
out vec3 v_Normal;

void main() {
    v_Normal = a_Normal;
    v_FragPos = vec3(u_ModelMatrix * vec4(a_Position, 1.0));
    gl_Position = u_ProjMatrix * u_ViewMatrix * u_ModelMatrix * vec4(a_Position, 1.0);
}