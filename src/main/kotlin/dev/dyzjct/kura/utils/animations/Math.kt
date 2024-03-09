/*
 * Copyright (c) 2021 CakeSlayers Reversing Team. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * CakeSlayers' Github website: https://github.com/CakeSlayers
 * This file was created by SagiriXiguajerry at 2021/11/21 下午7:47
 */
@file:Suppress("nothing_to_inline", "unused")

package dev.dyzjct.kura.utils.animations

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor

const val PI_FLOAT: Float = 3.1415927f

const val FLOOR_DOUBLE_D: Double = 1_073_741_824.0
const val FLOOR_DOUBLE_I: Int = 1_073_741_824

const val FLOOR_FLOAT_F: Float = 4_194_304.0f
const val FLOOR_FLOAT_I: Int = 4_194_304

inline fun Double.floorToInt(): Int = floor(this).toInt()
inline fun Float.floorToInt(): Int = floor(this).toInt()

inline fun Double.ceilToInt(): Int = ceil(this).toInt()
inline fun Float.ceilToInt(): Int = ceil(this).toInt()

inline fun Double.fastFloor(): Int = (this + FLOOR_DOUBLE_D).toInt() - FLOOR_DOUBLE_I
inline fun Float.fastFloor(): Int = (this + FLOOR_FLOAT_F).toInt() - FLOOR_FLOAT_I

inline fun Double.fastCeil(): Int = FLOOR_DOUBLE_I - (FLOOR_DOUBLE_D - this).toInt()
inline fun Float.fastCeil(): Int = FLOOR_FLOAT_I - (FLOOR_FLOAT_F - this).toInt()

inline fun Float.toRadian(): Float = this / 180.0f * PI_FLOAT
inline fun Double.toRadian(): Double = this / 180.0 * PI

inline fun Float.toDegree(): Float = this * 180.0f / PI_FLOAT
inline fun Double.toDegree(): Double = this * 180.0 / PI

inline val Double.sq: Double get() = this * this
inline val Float.sq: Float get() = this * this
inline val Int.sq: Int get() = this * this

inline val Double.cubic: Double get() = this * this * this
inline val Float.cubic: Float get() = this * this * this
inline val Int.cubic: Int get() = this * this * this

inline val Double.quart: Double get() = this * this * this * this
inline val Float.quart: Float get() = this * this * this * this
inline val Int.quart: Int get() = this * this * this * this

inline val Double.quint: Double get() = this * this * this * this * this
inline val Float.quint: Float get() = this * this * this * this * this
inline val Int.quint: Int get() = this * this * this * this * this