#version 300 es
#extension GL_OES_EGL_image_external_essl3: require
precision mediump float;

uniform samplerExternalOES u_TexUnitVideo;
uniform int u_Filter;


// Filters ordinals:
//   Colored: 0
//   Gray:    1
//   Invert:  2
//   Blur:    3 - отдельные program/shaders

in vec2 v_TextPos;
out vec4 FragColor;


const float DIV = 18.0;

vec4 grayScaleFilter(vec4 origColor) {
    float gray = (origColor.r + origColor.g + origColor.b) / 3.0;
    return vec4(gray, gray, gray, 1.0);
}

vec4 invertFilter(vec4 origColor) {
    return vec4((1.0 - origColor.rgb), 1.0);
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
        case 3:  // Blur - отдельные program/shaders
            FragColor = texture(u_TexUnitVideo, v_TextPos);
            break;
        default:  // Colored
            FragColor = texture(u_TexUnitVideo, v_TextPos);
    }
}