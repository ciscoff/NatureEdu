#version 300 es
precision mediump float;

#define PI 3.14159265359
#define TWO_PI 6.28318530718

struct Light {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

struct Material {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
};

uniform vec3 u_LightPos;
uniform vec3 u_ViewerPos;
uniform sampler2D u_TexUnitDay;
uniform sampler2D u_TexUnitNight;
uniform float u_Time;

in vec3 v_FragPos;
in vec2 v_TextPos;
in vec3 v_Normal;
out vec4 FragColor;

uniform Light u_Light;
uniform Material u_Material;

mat4 rotationX(in float angle) {
    return mat4(1.0, 0, 0, 0,
                0, cos(angle), -sin(angle), 0,
                0, sin(angle), cos(angle), 0,
                0, 0, 0, 1);
}

mat4 rotationY(in float angle) {
    return mat4(cos(angle), 0, sin(angle), 0,
                0, 1.0, 0, 0,
                -sin(angle), 0, cos(angle), 0,
                0, 0, 0, 1);
}

mat4 rotationZ(in float angle) {
    return mat4(cos(angle), -sin(angle), 0, 0,
                sin(angle), cos(angle), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
}

/**
  На что обратить внимание:
  Fragment shader работает в left handed системе координат (Z направленена от нас) и у каждого
  вертекса сменился знак Z (projection matrix). Это не значит, что вертекс "переместился в
  пространстве". Если он был в мире на переднем плане перед камерой, то он так и остается на
  переднем плане в NDC, но его Z поменяла знак.

  Однако это никак не влияет на наш расчет освещения текущего вертекса (фрагмента). Мы оперируем
  данными МИРА. В мире источник света находился в определенной позиции, вертекс (фрагмент) тоже.
  Их взаимное положение в МИРЕ влияло на результирующий цвет вертекса (фрагмента). Нам остается
  только вычислить этот цвет и применить к фрагменту.
 */
void main() {
    float lightRotationAngle = mod(u_Time * 0.2, TWO_PI);
    vec3 lightPos = (rotationY(lightRotationAngle) * vec4(u_LightPos, 1.0)).xyz;

    vec3 lightDir = normalize(lightPos - v_FragPos);
    float diffFactor = dot(v_Normal, lightDir);

    vec4 color = vec4(0.0);
    if(diffFactor < 0.0) {
        color = abs(diffFactor) * texture(u_TexUnitNight, v_TextPos);
    } else {
        color = diffFactor * texture(u_TexUnitDay, v_TextPos);
    }

    // ambient color
    float ambientFactor = 0.2; // темнее/светлее
    vec3 ambientColor = ambientFactor * u_Light.ambient * color.rgb;
    // diffuse color
    vec3 diffuseColor = (u_Light.diffuse * color.rgb);

    color = vec4(ambientColor + diffuseColor, 1.0);
    FragColor = color;
}