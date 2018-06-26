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

package io.iskylake.lakebot.commands.`fun`

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.EmbedBuilder.ZERO_WIDTH_SPACE
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class ChooseCommand : Command {
    override val name = "choose"
    override val aliases = listOf("random")
    override val description = "The command that randomly selects one of the specified arguments"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} argument1 | argument2 | ... | argumentN"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            val toChoose = arguments.split("(\\s+\\|+\\s+)|(\\|+)".toRegex())
            event.channel.run {
                try {
                    sendMessage(buildEmbed {
                        color { Immutable.SUCCESS }
                        description { toChoose.random() }
                    }).queue()
                } catch (e: Exception) {
                    sendMessage(buildEmbed {
                        color { Immutable.SUCCESS }
                        description { ZERO_WIDTH_SPACE }
                    }).queue()
                }
            }
        } else {
            event.sendError("You specified no content!").queue()
        }
    }
}