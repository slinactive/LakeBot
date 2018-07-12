/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
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
 */

package io.iskylake.lakebot.entities.extensions

import java.lang.Long.toBinaryString
import java.lang.Long.toHexString

import kotlin.experimental.inv

operator fun Byte.not() = inv()
operator fun Short.not() = inv()
operator fun Int.not() = inv()
operator fun Long.not() = inv()
fun Int.toByteArray() = this.toLong().toByteArray()
fun Int.toBinary() = this.toLong().toBinary()
fun Int.toHex() = this.toLong().toHex()
fun Long.toByteArray() = byteArrayOf((this ushr 24).toByte(), (this ushr 16).toByte(), (this ushr 8).toByte(), this.toByte())
fun Long.toBinary() = toBinaryString(this)
fun Long.toHex() = toHexString(this)
fun Int.Companion.random(range: IntRange) = range.toList().random()