#version 300 es
#extension GL_OES_EGL_image_external_essl3: require
precision mediump float;

uniform samplerExternalOES u_TexUnitVideo;
uniform int u_Effect;

in vec2 v_TextPos;
out vec4 FragColor;

vec4 grayScaleEffect(vec4 origColor) {
    vec4 color = texture(u_TexUnitVideo, v_TextPos);
    float gray = (color.r + color.g + color.b) / 3.0;
    return vec4(gray, gray, gray, 1.0);
}

void main() {

    vec4 color = vec4(1.0);

    switch (u_Effect) {
        case 1:
            FragColor = grayScaleEffect(texture(u_TexUnitVideo, v_TextPos));
            break;
        case 2:
            FragColor = texture(u_TexUnitVideo, v_TextPos);
            break;
        default:
            FragColor = texture(u_TexUnitVideo, v_TextPos);
    }
}