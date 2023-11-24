precision mediump float;

uniform vec3 u_LightPos;
uniform vec3 u_LightColor;

varying vec3 v_Color;
varying vec3 v_FragPos;
varying vec3 v_Normal;

void main() {
    vec3 lightDir = normalize(u_LightPos - v_FragPos);

    // Влияние рассеивания на свет от источника
    float diffFactor = max(dot(v_Normal, lightDir), 0.0);
    vec3 diffusion = diffFactor * u_LightColor;

    // Влияние освещения окружающей среды на свет от источника
    float ambientFactor = 0.1;
    vec3 ambient = ambientFactor * u_LightColor;

    // Влияние всех факторов на цвет объекта
    vec3 color = (ambient + diffusion) * v_Color;

    gl_FragColor = vec4(color, 1.0);
}