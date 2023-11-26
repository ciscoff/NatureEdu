uniform mat4 u_Matrix;

attribute vec3 a_Position;

varying vec3 v_Color;

void main() {
    v_Color = vec3(0.0, 0.0, 1.0);
    gl_Position = u_Matrix * vec4(a_Position, 1.0);
}