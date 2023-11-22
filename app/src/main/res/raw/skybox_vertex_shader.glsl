uniform mat4 u_Matrix;

attribute vec3 a_Position;

varying vec3 v_Position;

void main() {
    v_Position = a_Position;

    // Находясь внутри куба мы будем видеть "внешнюю" поверхность каждой его стороны.
    // Иначе будем видеть внутреннюю поверхность, которая есть зеркальное отражение внешней.
    v_Position.z = -v_Position.z;
    gl_Position = u_Matrix * vec4(a_Position, 1.0);

    // Эта строка помещает все позиции на far плоскость пирамиды потому что
    // последующая операция perspective divide всегда выдаст для z = 1
    gl_Position = gl_Position.xyww;
}