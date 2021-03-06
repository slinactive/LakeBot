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

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.argsStripped
import io.ilakeful.lakebot.entities.extensions.sendFailure
import io.ilakeful.lakebot.utils.ImageUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TextToImageCommand : Command {
    override val name = "tti"
    override val aliases = listOf("texttoimage", "text-to-image")
    override val description = "The command converting your text into an image"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <text>"
    override val cooldown = 3L
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsStripped !== null) {
            try {
                event.channel.sendFile(ImageUtils.getImagedText(event.argsStripped!!.split("\n")), "tti.png").queue()
            } catch (e: Exception) {
                event.channel.sendFailure("Something went wrong!").queue()
            }
        } else {
            event.channel.sendFailure("You haven't specified any arguments!").queue()
        }
    }
}