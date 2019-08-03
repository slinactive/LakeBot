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

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class SayCommand : Command {
    override val name = "say"
    override val aliases = listOf("announce")
    override val description = "The command sending your message on behalf of LakeBot"
    override val usage: (String) -> (String) = { "${super.usage(it)} <content>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            if (event.message.attachments.none { it.isImage }) {
                buildEmbed {
                    author(event.author.tag) {
                        event.author.effectiveAvatarUrl
                    }
                    color {
                        Immutable.SUCCESS
                    }
                    description {
                        event.argsRaw!!
                    }
                }.let {
                    event.sendMessage(it).queue {
                        try {
                            event.message.delete().queue()
                        } catch (ignored: Exception) {
                        }
                    }
                }
            } else {
                val image = event.message.attachments.first { it.isImage }
                buildEmbed {
                    image {
                        image.url
                    }
                    author(event.author.tag) {
                        event.author.effectiveAvatarUrl
                    }
                    color {
                        Immutable.SUCCESS
                    }
                    description {
                        event.argsRaw!!
                    }
                }.let {
                    event.sendMessage(it).queue()
                }
            }
        } else {
            if (event.message.attachments.any { it.isImage }) {
                val image = event.message.attachments.first { it.isImage }
                buildEmbed {
                    image {
                        image.url
                    }
                    author(event.author.tag) {
                        event.author.effectiveAvatarUrl
                    }
                    color {
                        Immutable.SUCCESS
                    }
                }.let {
                    event.sendMessage(it).queue()
                }
            } else {
                event.sendFailure("You specified no content!").queue()
            }
        }
    }
}