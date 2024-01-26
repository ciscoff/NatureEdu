#version 300 es
#extension GL_OES_EGL_image_external_essl3: require
precision mediump float;

uniform samplerExternalOES u_TexUnitVideo;
uniform int u_Filter;

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

// Blur усиливается усилением увеличением kernel матрицы. Сетка 3x3 дает слабый результат.
vec4 blurEffect() {
    vec3 sampleTex[9];

    vec3 color = vec3(0.0);

    vec2 offsets[9] = vec2[](
        vec2(-offset, offset), // top-left
        vec2(0.0f, offset), // top-center
        vec2(offset, offset), // top-right
        vec2(-offset, 0.0f), // center-left
        vec2(0.0f, 0.0f), // center-center
        vec2(offset, 0.0f), // center-right
        vec2(-offset, -offset), // bottom-left
        vec2(0.0f, -offset), // bottom-center
        vec2(offset, -offset)  // bottom-right
    );

    float kernel[9] = float[](
        1.0 / DIV, 1.0 / DIV, 1.0 / DIV,
        1.0 / DIV, 10.0 / DIV, 1.0 / DIV,
        1.0 / DIV, 1.0 / DIV, 1.0 / DIV
    );

    for (int i = 0; i < 9; i++) {
        sampleTex[i] = vec3(texture(u_TexUnitVideo, v_TextPos.st + offsets[i]));
        color += sampleTex[i] * kernel[i];
    }

    return vec4(color, 1.0);
}

vec4 blurEffect2() {

    vec3 color = vec3(0.0);
    vec3 sampleTex[25];
    vec2 offsets[25];

    for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {

            int ii = i - 2;
            int jj = j - 2;

            offsets[i * 5 + j] = vec2(float(ii) * offset, float(jj) * offset);
        }
    }

    float kernel[25] = float[](
        .00001, .00043, .00171, .00043, .00001,
        .00043, .02749, .11024, .02749, .00043,
        .00171, .11024, .44210, .11024, .00171,
        .00043, .02749, .11024, .02749, .00043,
        .00001, .00043, .00171, .00043, .00001
    );

    for (int i = 0; i < 25; i++) {
        sampleTex[i] = vec3(texture(u_TexUnitVideo, v_TextPos.st + offsets[i]));
        color += sampleTex[i] * kernel[i];
    }

    return vec4(color, 1.0);
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
        case 3:
            FragColor = blurEffect2();
            break;
        default:  // Colored
            FragColor = texture(u_TexUnitVideo, v_TextPos);
    }
}