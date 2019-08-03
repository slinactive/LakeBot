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

package io.ilakeful.lakebot.commands.`fun`

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.internal.utils.Helpers

class ChooseCommand : Command {
    override val name = "choose"
    override val aliases = listOf("random")
    override val description = "The command selecting one of the specified options in a random way"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} option1 | option2 | ... | optionN"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            event.channel.sendMessage(buildEmbed {
                val toChoose = arguments.split("(\\s+\\|+\\s+)|(\\|+)".toRegex())
                val random = toChoose.random()
                val toSend = if (Helpers.isBlank(random)) ZERO_WIDTH_SPACE else random
                color { Immutable.SUCCESS }
                description { toSend }
            }).queue()
        } else {
            event.sendFailure("You specified no content!").queue()
        }
    }
}