uniform mat4 u_Matrix;
uniform vec3 u_VectorToLight;

attribute vec3 a_Position;
attribute vec3 a_Normal;

varying vec3 v_Color;

void main() {
    // ambient lighting factor
    float ambientFactor = 0.15;

    v_Color = mix(vec3(0.180, 0.467, 0.153), // A dark green
                  vec3(0.660, 0.670, 0.680), // A stony gray
                  a_Position.y);

    // diffuse lighting factor
    float diffuseFactor = max(dot(a_Normal, u_VectorToLight), 0.0);

//    v_Color *= (diffuseFactor + ambientFactor);

    // Такой алгоритм дает эффект лунного света.
    // Чем больше значение ambientFactor, тем больше "молока"
    v_Color *= diffuseFactor;
    v_Color += ambientFactor;

    gl_Position = u_Matrix * vec4(a_Position, 1.0);
}
