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

package io.ilakeful.lakebot.entities.extensions

val Regex.Companion.DISCORD_ID
    get() = "\\d{17,20}".toRegex()
val Regex.Companion.DISCORD_USER
    get() = "<@!?($DISCORD_ID)>".toRegex()
val Regex.Companion.DISCORD_ROLE
    get() = "<@&($DISCORD_ID)>".toRegex()
val Regex.Companion.DISCORD_CHANNEL
    get() = "<#($DISCORD_ID)>".toRegex()
val Regex.Companion.DISCORD_EMOTE
    get() = "<(?:a)?:(\\S{2,32}):($DISCORD_ID)>".toRegex()