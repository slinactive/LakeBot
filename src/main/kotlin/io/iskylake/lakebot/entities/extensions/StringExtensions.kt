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

infix fun String.append(str: String): String = "$this$str"
infix fun String.appendln(str: String): String = this.append("$str\n")
infix fun String.prepend(str: String): String = "$str$this"
infix fun String.prependln(str: String): String = this.append("\n$str")
infix fun String.matches(regex: String): Boolean = this.matches(regex.toRegex())
val String.isByte: Boolean
    get() = try {
        toByte()
        true
    } catch (e: Exception) {
        false
    }
val String.isShort: Boolean
    get() = try {
        toShort()
        true
    } catch (e: Exception) {
        false
    }
val String.isInt: Boolean
    get() = try {
        toInt()
        true
    } catch (e: Exception) {
        false
    }
val String.isLong: Boolean
    get() = try {
        toLong()
        true
    } catch (e: Exception) {
        false
    }
fun String.capitalizeAll(isForce: Boolean = false): String {
    val str: String = if (isForce) this.toLowerCase() else this
    val chars: CharArray = str.toCharArray()
    var found = false
    for (i in 0 until chars.size) {
        if (!found && Character.isLetter(chars[i])) {
            chars[i] = Character.toUpperCase(chars[i])
            found = true
        }
        else if (Character.isWhitespace(chars[i]) || chars[i] == '.' || chars[i] == '\'') {
            found = false
        }
    }
    return chars.joinToString("")
}
operator fun String.times(num: Int): String = buildString {
    repeat(num) {
        append(this@times)
    }
}
fun String.safeSubstring(begin: Int, end: Int = this.count()) = try {
    this.substring(begin, end)
} catch (t: Throwable) {
    this
}
fun String.escapeDiscordMarkdown(): String = this.replace("*", "\\*").replace("_", "\\_").replace("~", "\\~").replace("`", "\\`")
