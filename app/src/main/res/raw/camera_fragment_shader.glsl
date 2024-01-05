#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

uniform samplerExternalOES u_TexUnitVideo;

in vec2 v_TextPos;
out vec4 FragColor;

void main() {
    FragColor = texture(u_TexUnitVideo, v_TextPos);
}