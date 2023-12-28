precision mediump float;

uniform vec3 u_LightPos;
uniform vec3 u_LightColor;

uniform vec3 u_Color;
uniform int u_Illuminated;
uniform vec2 u_Resolution;

varying vec3 v_FragPos;
varying vec3 v_Normal;

// Нормализуем позицию фрагмента и интерпретируем как его цвет.
// Должны получить сферу с таким распределением цветов:
//
//  Результат зависит от значения Z.
//
//  Если не иинвертировать Z.  Если иинвертировать Z (правильно)
//
//       Green                     Green
//       Black  Red                Blue   Red
//   Black                     Blue
//
vec3 fragPosToColor(vec3 fragPos) {
//    return normalize(fragPos) * 0.5 + 0.5;
    return normalize(fragPos);
}

vec3 fragPosXToColor(vec3 fragPos) {
    return vec3(normalize(fragPos).x);
}

vec3 fragPosYToColor(vec3 fragPos) {
    return vec3(normalize(fragPos).y);
}

vec3 fragPosZToColor(vec3 fragPos) {
    return vec3(normalize(fragPos).z);
}

vec3 normToColor(vec3 norm) {
//    return normalize(norm) * 0.5 + 0.5;
    return normalize(norm);
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
    vec2 st = gl_FragCoord.xy/u_Resolution.xy;
    vec3 color = u_Color;

    // Тут можно поиграться для отладки и разложить по цветам различные униформы и атрибуты.
//        color = fragPosToColor(vec3(v_FragPos.x, v_FragPos.y, v_FragPos.z));
//        color = normToColor(vec3(v_Normal.x, v_Normal.y, v_Normal.z));
//    color = fragPosXToColor(v_FragPos);
//    color = fragPosZToColor(v_FragPos);
//    color = vec3(v_FragPos.z);

    if (u_Illuminated == 1) {
        vec3 lightDir = normalize(u_LightPos - v_FragPos);

        // Влияние рассеивания на свет от источника
        float diffFactor = max(dot(v_Normal, lightDir), 0.0);

        vec3 diffusion = diffFactor * u_LightColor;
        color = diffusion * u_Color;
    }
    gl_FragColor = vec4(color, 1.0);
}