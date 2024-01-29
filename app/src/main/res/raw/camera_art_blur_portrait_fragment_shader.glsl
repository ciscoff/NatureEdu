#version 300 es
#extension GL_OES_EGL_image_external_essl3: require
precision mediump float;

// Это значение нужно подстраивать при изменении размера u_BlurKernel. При увеличении
// ярла уменьшаем COLOR_SCALE.
// Примеры:
//
// kernel:5  ->  0.6
// kernel:7  ->  0.43
// kernel:9  ->  0.34
// kernel:11 ->  0.28
// kernel:15 ->  0.22
// kernel:21 ->  0.145

// NOTE:
// Хороший способ выделить блюр  - это его затемнить. Тогда при меньшем kernel достигается
// неплоходй визуальный эффект.
#define COLOR_SCALE 0.32

uniform samplerExternalOES u_OesTexSampler;
uniform sampler2D u_FboTexSampler;
uniform bool u_Horizontal;
uniform bool u_FirstIteration;

// Размер массива должен быть 'u_BlurRadius + 1'. См. параметр radius в BlurProgram.
uniform float u_BlurKernel[9];
uniform int u_BlurRadius;

in vec2 v_TextPos;
out vec4 FragColor;

/**
 * OES_Tex используем только на первой итерации и ее текселы блюрим горизонтально.
 * На следующей итерации будем брать текселы из текстуры FBO и будем их блюрить вертикально.
 * И далее работаем с текстурами FBO поочередно блюря их горизонтально/вертикально.
*/
vec4 sampleFromOes(bool isNotBluring) {
    vec3 result = vec3(0.0);
    float factor = COLOR_SCALE;

    ivec2 texSize = textureSize(u_OesTexSampler, 0);
    vec2 tex_offset = vec2(1.0 / float(texSize.x), 1.0 / float(texSize.y));

    if (isNotBluring) {
        result = texture(u_OesTexSampler, v_TextPos).rgb;
        factor = 1.;
    } else {
        result = texture(u_OesTexSampler, v_TextPos).rgb * u_BlurKernel[0];

        for (int i = 1; i < u_BlurRadius + 1; ++i) {
            result += texture(u_OesTexSampler, v_TextPos + vec2(tex_offset.x * float(i), 0.0)).rgb * u_BlurKernel[i];
            result += texture(u_OesTexSampler, v_TextPos - vec2(tex_offset.x * float(i), 0.0)).rgb * u_BlurKernel[i];
        }
    }

    return vec4(result * factor, 1.0);
}

vec4 sampleFromFbo(bool isNotBluring) {
    vec3 result = vec3(0.0);
    int count = u_BlurRadius + 1;
    float factor = COLOR_SCALE;

    ivec2 texSize = textureSize(u_FboTexSampler, 0);
    vec2 tex_offset = vec2(1.0 / float(texSize.x), 1.0 / float(texSize.y));

    if (isNotBluring) {
        result = texture(u_FboTexSampler, v_TextPos).rgb;
        factor = 1.;
    } else {
        result = texture(u_FboTexSampler, v_TextPos).rgb * u_BlurKernel[0];

        if (u_Horizontal) {
            for (int i = 1; i < count; ++i) {
                result += texture(u_FboTexSampler, v_TextPos + vec2(tex_offset.x * float(i), 0.0)).rgb * u_BlurKernel[i];
                result += texture(u_FboTexSampler, v_TextPos - vec2(tex_offset.x * float(i), 0.0)).rgb * u_BlurKernel[i];
            }
        }
        else {
            for (int i = 1; i < count; ++i) {
                result += texture(u_FboTexSampler, v_TextPos + vec2(0.0, tex_offset.y * float(i))).rgb * u_BlurKernel[i];
                result += texture(u_FboTexSampler, v_TextPos - vec2(0.0, tex_offset.y * float(i))).rgb * u_BlurKernel[i];
            }
        }
    }

    return vec4(result * factor, 1.0);
}

void main() {

    // Блюрим сверху и снизу, оставляя центр оригинальным. Здесь используем v_TextPos.x вместо
    // v_TextPos.y потому что работаем с оригинальным raw-кадром из камеры у которого координатная
    // система как у сенсора. То есть повернута.
    bool isNotBluring = v_TextPos.x > 0.2 && v_TextPos.x < 0.8;

    if (u_FirstIteration) {
        FragColor = sampleFromOes(isNotBluring);
    } else {
        FragColor = sampleFromFbo(isNotBluring);
    }
}