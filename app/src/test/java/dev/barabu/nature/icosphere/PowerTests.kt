package dev.barabu.nature.icosphere

import kotlin.math.pow

val levels = arrayOf(0, 1, 2, 3)
val triangles = arrayOf(20, 80, 320, 1280)

//
//   level     | 0    | 1   |  2    |  3    | ...
//   ----------|------|-----|-------|-------|---
//   triangles | 20   | 80  |  320  | 1280  |
//
//    20 = 20 * 1  = 20 * 2^0
//    80 = 20 * 4  = 20 * 2^2
//   320 = 20 * 16 = 20 * 2^4
//  1280 = 20 * 64 = 20 * 2^6
//
//  Получается, что степени 2 идут в последовательности 0, 2, 4, 6,...,
//  то есть связаны с параметром level как (level*2)
fun main() {

    for (level in levels) {
        println("level=$level, ${20 * 2.0.pow(level * 2)}")
    }
}