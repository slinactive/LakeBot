/*
 * Copyright 2017-2019 (c) Alexander "ILakeful" Shevchenko
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
 *  limitations under the License.
 */

package io.ilakeful.lakebot.entities

data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int = 0,
        val stability: Stability = Stability.STABLE,
        val unstable: Int? = null
) {
    override fun toString() = arrayOf(
            major,
            minor,
            if (patch == 0) null else patch
    ).filterNotNull().joinToString(separator = ".") + stability.let {
        val suffix = it.suffix
        if (suffix !== null && (unstable !== null && unstable != 0)) {
            "-$suffix$unstable"
        } else ""
    }
    enum class Stability(val suffix: String?) {
        STABLE(null), BETA("BETA"), ALPHA("ALPHA")
    }
}