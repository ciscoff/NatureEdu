uniform mat4 u_MvpMatrix;
uniform mat4 u_ModelMatrix;

attribute vec3 a_Position;
attribute vec3 a_Normal;

varying vec3 v_FragPos;
varying vec3 v_Normal;

void main() {
    v_Normal = a_Normal;
    v_FragPos = vec3(u_ModelMatrix * vec4(a_Position, 1.0));
    gl_Position = u_MvpMatrix * vec4(a_Position, 1.0);
}