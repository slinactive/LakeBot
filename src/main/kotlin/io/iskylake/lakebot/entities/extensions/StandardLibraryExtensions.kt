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
import java.lang.String.format as javaFormat
import java.util.Random

import kotlin.experimental.inv
import kotlin.text.Regex as RegularExpression

// Array
fun <T> Array<T>.random() = this[Random().nextInt(size)]
// Boolean
fun Boolean.asString() = if (this) "Yes" else "No"
// Byte
operator fun Byte.not() = inv()
// Char
val Char.name: String?
    get() = Char.getName(this)
fun Char.Companion.getName(value: Int): String? = Character.getName(value)
fun Char.Companion.getName(value: Char): String? = getName(value.toInt())
// Int
operator fun Int.not() = inv()
fun Int.toByteArray() = toLong().toByteArray()
fun Int.toBinary() = toLong().toBinary()
fun Int.toHex() = toLong().toHex()
fun Int.Companion.random(range: IntRange) = range.random()
// List
fun <T> List<T>.random() = this[Random().nextInt(size)]
// Long
operator fun Long.not() = inv()
fun Long.toByteArray() = byteArrayOf((this shr 24 and 0xff).toByte(), (this shr 16 and 0xff).toByte(), (this shr 8 and 0xff).toByte(), (this and 0xff).toByte())
fun Long.toBinary() = toBinaryString(this)
fun Long.toHex() = toHexString(this)
// Range
fun IntRange.random() = toList().random()
// Short
operator fun Short.not() = inv()
// String
infix fun String.append(str: String): String = "$this$str"
infix fun String.appendln(str: String): String = append("$str\n")
infix fun String.prepend(str: String): String = "$str$this"
infix fun String.prependln(str: String): String = append("\n$str")
val String.isByte: Boolean
    get() = toByteOrNull() !== null
val String.isShort: Boolean
    get() = toShortOrNull() !== null
val String.isInt: Boolean
    get() = toIntOrNull() !== null
val String.isLong: Boolean
    get() = toLongOrNull() !== null
fun String.capitalizeAll(isForce: Boolean = false): String {
    val str = if (isForce) toLowerCase() else this
    val chars = str.toCharArray()
    var found = false
    for (i in 0 until chars.size) {
        if (!found && chars[i].isLetter()) {
            chars[i] = chars[i].toUpperCase()
            found = true
        } else if (chars[i].isWhitespace() || chars[i] == '.' || chars[i] == '\'') {
            found = false
        }
    }
    return String(chars)
}
operator fun String.times(num: Int) = buildString {
    repeat(num) {
        append(this@times)
    }
}
fun String.format(vararg args: Any) = javaFormat(this, *args)
fun String.safeSubstring(begin: Int = 0, end: Int = this.count()) = try {
    substring(begin, end)
} catch (t: Throwable) {
    this
}
fun String.removeContent(content: String) = replace(content, "")
fun String.removeContent(regex: RegularExpression) = replace(regex, "")
fun String.escapeDiscordMarkdown() = replace("*", "\\*").replace("_", "\\_").replace("~", "\\~").replace("`", "\\`")