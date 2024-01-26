#version 300 es
#extension GL_OES_EGL_image_external_essl3: require
precision mediump float;

uniform samplerExternalOES u_TexUnitVideo;
uniform int u_Filter;

// Размер маассива должен быть не меньше, чем (2* BLUR_RADIUS + 1) * (2* BLUR_RADIUS + 1)
uniform float u_BlurKernel[49];
uniform int u_BlurRadius;

// Filters ordinals:
//   Colored: 0
//   Gray: 1
//   Invert: 2
//   Blur: 3

in vec2 v_TextPos;
out vec4 FragColor;

// Kernel effects offsets
const float offset = 1.0 / 300.0;

const float DIV = 18.0;

vec4 grayScaleFilter(vec4 origColor) {
    float gray = (origColor.r + origColor.g + origColor.b) / 3.0;
    return vec4(gray, gray, gray, 1.0);
}

vec4 invertFilter(vec4 origColor) {
    return vec4((1.0 - origColor.rgb), 1.0);
}

// Двухпроходный Blur.
// Дает низкое качество при малой размернойсти ядра и сильно тормозит при увеличении ядра.
// Например при ядре 11x11 ацки лагает.
vec4 twoPassBlur(vec2 pos) {
    vec3 result = vec3(0.);

    float offset = 1. / 400.;

    int kernelDim = 2 * u_BlurRadius + 1;

    // генерим цвет
    for(int i = 0; i < kernelDim; i++) {
        float ii = float(i) - float(u_BlurRadius);

        for(int j = 0; j < kernelDim; j++) {
            float jj = float(j) - float(u_BlurRadius);

            result += texture(u_TexUnitVideo, pos + vec2(ii * offset, jj * offset)).rgb * u_BlurKernel[i * kernelDim + j];
        }
    }

    return vec4(result * 0.4, 1.);
}

void main() {

    vec4 color = vec4(1.0);

    switch (u_Filter) {
        case 1:  // Grayscale
            FragColor = grayScaleFilter(texture(u_TexUnitVideo, v_TextPos));
            break;
        case 2:  // Invert
            FragColor = invertFilter(texture(u_TexUnitVideo, v_TextPos));
            break;
        case 3:  // Blur
            FragColor = twoPassBlur(v_TextPos);
            break;
        default:  // Colored
            FragColor = texture(u_TexUnitVideo, v_TextPos);
    }
}