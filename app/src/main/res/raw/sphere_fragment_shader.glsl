precision mediump float;

uniform vec3 u_LightPos;
uniform vec3 u_LightColor;

uniform vec3 u_Color;

varying vec3 v_FragPos;
varying vec3 v_Normal;

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
    vec3 color = u_Color;

    vec3 lightDir = normalize(u_LightPos - v_FragPos);

    // Учитываем угол падения света на поверхность фрагмента. Это будет влиять на
    // интенсивность освещения.
    float diffFactor = max(dot(v_Normal, lightDir), 0.0);

    vec3 diffusion = diffFactor * u_LightColor;
    color = diffusion * u_Color;

    gl_FragColor = vec4(color, 1.0);
}