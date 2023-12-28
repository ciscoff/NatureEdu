#version 300 es
precision mediump float;

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
uniform sampler2D u_TexUnit;

in vec3 v_FragPos;
in vec2 v_TextPos;
in vec3 v_Normal;
out vec4 FragColor;

uniform Light u_Light;
uniform Material u_Material;

uniform vec3 u_Color;
uniform int u_DrawPolygon;

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
    vec4 color = vec4(u_Color, 1.0);

    if (u_DrawPolygon == 1) {

        color = texture(u_TexUnit, v_TextPos) * vec4(0.7, 0.7, 0.7, 1.);

        // ambient color
        float ambientFactor = 0.8; // темнее/светлее
        vec3 ambientColor = ambientFactor * u_Light.ambient * color.rgb;

        // diffuse color
        vec3 lightDir = normalize(u_LightPos - v_FragPos);
        float diffFactor = max(dot(v_Normal, lightDir), 0.0);
        vec3 diffuseColor = (diffFactor * u_Light.diffuse * color.rgb);

        // specular color
        vec3 viewDir = normalize(u_ViewerPos - v_FragPos);
        vec3 reflectDir = reflect(-lightDir, v_Normal);
        float specFactor = pow(max(dot(viewDir, reflectDir), 0.0), u_Material.shininess);
        vec3 specularColor = (specFactor * u_Light.specular);

        color = vec4(ambientColor + diffuseColor + specularColor, 1.0);
    }

    // NOTE: texture2D is deprecated and changed to 'texture' between glsl 120 and 130
    FragColor = color;
}