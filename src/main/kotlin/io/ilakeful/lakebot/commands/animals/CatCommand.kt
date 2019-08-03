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

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import org.jsoup.Jsoup
import org.jsoup.parser.Parser

class CatCommand : Command {
    val image: String
        inline get() {
            val xml = Jsoup.connect("http://thecatapi.com/api/images/get?format=xml&results_per_page=1").get()
            val doc = Jsoup.parse(xml.allElements.html(), "", Parser.xmlParser())
            return doc.select("url").first().text()
        }
    override val name = "cat"
    override val aliases = listOf("kitty", "kitten")
    override val description = "The command that sends random cat image"
    override val cooldown = 3L
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val embed = buildEmbed {
            color { Immutable.SUCCESS }
            image { image }
            footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
        }
        event.channel.sendMessage(embed).queue()
    }
}