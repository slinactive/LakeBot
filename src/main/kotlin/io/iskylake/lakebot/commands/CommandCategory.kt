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
 * limitations under the License.
 */

package io.iskylake.lakebot.commands

enum class CommandCategory(val category: String) {
    ANIMALS("Animals"),
    MODERATION("Moderation"),
    UTILS("Utils"),
    GENERAL("General"),
    DEVELOPER("Developer"),
    AUDIO("Audio"),
    INFO("Info"),
    FUN("Fun"),
    BETA("Beta"),
    UNKNOWN("Unknown");
    override fun toString(): String = this.category
    operator fun invoke(): String = this.category
    companion object {
        @JvmStatic
        operator fun iterator() = CommandCategory.values().iterator()
        @JvmStatic
        operator fun get(value: String) = CommandCategory.values().firstOrNull {
            it.category.toLowerCase() == value.toLowerCase()
        }
        @JvmStatic
        operator fun contains(value: String) = value.toLowerCase() in CommandCategory.values().map {
            it.category.toLowerCase()
        }
    }
}