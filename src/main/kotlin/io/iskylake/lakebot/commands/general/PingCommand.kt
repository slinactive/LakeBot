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

package io.iskylake.lakebot.commands.general

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class PingCommand : Command {
    override val name = "ping"
    override val aliases = listOf("delay", "response")
    override val description = "The command that sends the bot's current response time off from statistics"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val now = System.currentTimeMillis()
        event.channel.sendMessage("Calculating...").queue {
            buildEmbed {
                field(true, "Rest Ping") { "${System.currentTimeMillis() - now} ms" }
                field(true, "WebSocket Ping") { "${event.jda.ping} ms" }
                color { Immutable.SUCCESS }
            }.run {
                it.editMessage(this).override(true).queue()
            }
        }
    }
}