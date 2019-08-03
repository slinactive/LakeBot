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

package io.ilakeful.lakebot.commands.animals

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import khttp.get

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class DogCommand : Command {
    tailrec fun getImage(): String {
        val text = get("https://random.dog/woof", headers = mapOf()).text
        return if (".mp4" in text) getImage() else text
    }
    override val name = "dog"
    override val aliases = listOf("woof", "puppy", "pup")
    override val description = "The command that sends random dog image"
    override val cooldown = 3L
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val embed = buildEmbed {
            color { Immutable.SUCCESS }
            image { "https://random.dog/${getImage()}" }
            footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
        }
        event.channel.sendMessage(embed).queue()
    }
}