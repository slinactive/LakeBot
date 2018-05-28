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

package io.iskylake.lakebot.commands

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

interface Command {
    val name: String
    val aliases: List<String>
        get() = emptyList()
    val examples: (String) -> Map<String, String>
        get() = { _ -> emptyMap() }
    val category: CommandCategory
        get() {
            val args = this.javaClass.`package`.name.split("\\.".toRegex())
            return CommandCategory[args.last()] ?: CommandCategory.UNKNOWN
        }
    val isDeveloper: Boolean
        get() = category == CommandCategory.DEVELOPER
    val cooldown: Long
        get() = 0
    val description: String
    val usage: (String) -> String
        get() = { "$it$name" }
    suspend operator fun invoke(event: MessageReceivedEvent, args: Array<String>)
}