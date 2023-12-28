uniform mat4 u_Matrix;
uniform mat4 u_ModelMatrix;

attribute vec3 a_Position;
attribute vec3 a_Normal;

varying vec3 v_Color;
varying vec3 v_FragPos;
varying vec3 v_Normal;

void main() {
    v_Color = mix(vec3(0.180, 0.467, 0.153), // A dark green
                  vec3(0.660, 0.670, 0.680), // A stony gray
                  a_Position.y);

    gl_Position = u_Matrix * vec4(a_Position, 1.0);
    v_FragPos = vec3(u_ModelMatrix * vec4(a_Position, 1.0));
    v_Normal = a_Normal;
}