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
uniform vec2 u_Resolution;
uniform sampler2D u_TexUnitDay;
uniform sampler2D u_TexUnitNight;
uniform sampler2D u_TexUnitClouds;
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

float random(in vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// Value noise by Inigo Quilez - iq/2013
// https://www.shadertoy.com/view/lsf3WH
float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(random(i + vec2(0.0, 0.0)),
                   random(i + vec2(1.0, 0.0)), u.x),
               mix(random(i + vec2(0.0, 1.0)),
                   random(i + vec2(1.0, 1.0)), u.x), u.y);
}

mat2 rotate2d(float angle) {
    return mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
}

float plot(vec2 st, float pct) {
    return smoothstep(pct - 0.04, pct, st.y) - smoothstep(pct, pct + 0.04, st.y);
}

float slopeFromT(float t, float A, float B, float C) {
    float dtdx = 1.0 / (3.0 * A * t * t + 2.0 * B * t + C);
    return dtdx;
}

float xFromT(float t, float A, float B, float C, float D) {
    float x = A * (t * t * t) + B * (t * t) + C * t + D;
    return x;
}

float yFromT(float t, float E, float F, float G, float H) {
    float y = E * (t * t * t) + F * (t * t) + G * t + H;
    return y;
}

// https://thebookofshaders.com/edit.php?log=160414041933
float cubicBezier(float x, vec2 a, vec2 b) {

    float y0a = 0.0; // initial y
    float x0a = 0.0; // initial x
    float y1a = a.y;    // 1st influence y
    float x1a = a.x;    // 1st influence x
    float y2a = b.y;    // 2nd influence y
    float x2a = b.x;    // 2nd influence x
    float y3a = 1.0; // final y
    float x3a = 1.0; // final x

    float A = x3a - 3.0 * x2a + 3.0 * x1a - x0a;
    float B = 3.0 * x2a - 6.0 * x1a + 3.0 * x0a;
    float C = 3.0 * x1a - 3.0 * x0a;
    float D = x0a;

    float E = y3a - 3.0 * y2a + 3.0 * y1a - y0a;
    float F = 3.0 * y2a - 6.0 * y1a + 3.0 * y0a;
    float G = 3.0 * y1a - 3.0 * y0a;
    float H = y0a;

    // Solve for t given x (using Newton-Raphelson), then solve for y given t.
    // Assume for the first guess that t = x.
    float currentt = x;
    for (int i = 0; i < 5; i++) {
        float currentx = xFromT(currentt, A, B, C, D);
        float currentslope = slopeFromT(currentt, A, B, C);
        currentt -= (currentx - x) * (currentslope);
        currentt = clamp(currentt, 0.0, 1.0);
    }

    float y = yFromT(currentt, E, F, G, H);
    return y;
}

// https://thebookofshaders.com/edit.php#05/cubicpulse.frag
float smoothAscent(float c, float w, float x) {
    float offset = abs(c - x);
    if (offset > w) {
        if (x < c) return 0.0;
        else return 1.0;
    }
    return smoothstep(c - w, c + w, x);
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
    vec4 color = vec4(0.0);
    vec4 colorDay = vec4(0.0);
    vec4 colorNight = vec4(0.0);

    float lightRotationAngle = mod(u_Time * 0.2, TWO_PI);
    vec3 lightPos = (rotationY(lightRotationAngle) * vec4(u_LightPos, 1.0)).xyz;

    vec3 lightDir = normalize(lightPos - v_FragPos);
    float dayNightFactor = dot(v_Normal, lightDir); // [-1.0, +1,0]

    vec2 pos = v_TextPos;
    // Анимация облаков пока не работает
    //        pos = rotate2d( noise(pos * u_Time * 0.01) ) * pos;
    //        color = diffFactor * mix(texture(u_TexUnitDay, v_TextPos), texture(u_TexUnitClouds, pos), 0.2);
    colorDay = mix(texture(u_TexUnitDay, v_TextPos), texture(u_TexUnitClouds, pos), 0.2);
    colorNight = texture(u_TexUnitNight, v_TextPos);

    // dayNightFactor [-1.0, +1.0] -> normDayNightFactor [0.0, +1.0]
    // Полученный normDayNightFactor имеет плавную траекторию от [0.0, 0.0] к [1.0, 1.0]
    // и из-за этого нет явно выраженной границы между день/ночь. Нужно "сплющить" эту
    // траекторию по горизонтали с помощью Cubic Bezier.
    float normDayNightFactor = (dayNightFactor + 1.0) / 2.0;

    // Способ 1 [Cubic Bezier]
    // Контрольные точки Cubic Bezier.
    // Подобраны чтобы в центре был крутой вертикальный переход от 0.0 к 1.0
//    vec2 cp0 = vec2(0.9, 0.0);
//    vec2 cp1 = vec2(0.1, 1.0);
//    dayNightFactor = cubicBezier(normDayNightFactor, cp0, cp1);

    // Способ 2 [Продвинутый smoothstep]
    dayNightFactor = smoothAscent(0.5, 0.1, normDayNightFactor);

    color = mix(colorNight, colorDay, dayNightFactor);

    // Рисуем распределение значений normDiff/diffFactor.
    // Рисование diffFactor наглядно показывает рапределение день/ночь
    //    color = vec4(normDiff, normDiff, normDiff, 1.0);
    //    color = vec4(diffFactor, diffFactor, diffFactor, 1.0);

    // ambient color
    float ambientFactor = 0.2; // темнее/светлее
    vec3 ambientColor = ambientFactor * u_Light.ambient * color.rgb;
    // diffuse color
    vec3 diffuseColor = (u_Light.diffuse * color.rgb);

    color = vec4(ambientColor + diffuseColor, 1.0);
    FragColor = color;
}