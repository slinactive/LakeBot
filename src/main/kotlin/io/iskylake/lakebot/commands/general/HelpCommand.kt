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
import io.iskylake.lakebot.commands.CommandCategory
import io.iskylake.lakebot.commands.`fun`.AkinatorCommand
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.entities.handlers.CommandHandler

import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class HelpCommand : Command {
    override val name = "help"
    override val description = "N/A"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (args.isEmpty()) {
            val embed = buildEmbed {
                color {
                    Immutable.SUCCESS
                }
                author("${event.selfUser.name} Help:") {
                    event.selfUser.effectiveAvatarUrl
                }
                description {
                    "**[Support Server](${Immutable.SUPPORT_INVITE})** \u2022 **[Invite Link](https://discordapp.com/api/oauth2/authorize?client_id=${event.selfUser.id}&permissions=${Immutable.PERMISSIONS}&scope=bot)** \u2022 **[GitHub Repository](${Immutable.GITHUB_REPOSITORY})**"
                }
                timestamp()
                footer(event.author.effectiveAvatarUrl) {
                    "Requested by ${event.author.tag}"
                }
                val categories = mutableMapOf<CommandCategory, List<Command>>()
                for (command in CommandHandler.registeredCommands) {
                    val category = command.category
                    var edit = categories[category]
                    if (edit == null) {
                        edit = mutableListOf()
                    }
                    edit += command
                    categories += category to edit
                }
                val commandFields = mutableListOf<MessageEmbed.Field>()
                for ((category, commands) in categories) {
                    val list = commands.map { it.name }.sorted().joinToString()
                    commandFields += MessageEmbed.Field("${category().capitalize()} Commands:", list, false)
                }
                for (field in commandFields) {
                    field {
                        field
                    }
                }
            }
            event.message.addReaction(event.jda.getEmoteById(397757496447729664)).queue()
            event.author.privateChannel.sendMessage(embed).queue(null) {
                event.channel.sendMessage(embed).queue()
            }
        } else {
            val command = CommandHandler[args[0]]
            if (command !== null) {
                if (command !is HelpCommand) {
                    val embed = buildEmbed {
                        color {
                            Immutable.SUCCESS
                        }
                        author("${Immutable.DEFAULT_PREFIX}${command.name}") {
                            event.selfUser.effectiveAvatarUrl
                        }
                        field(title = "Description:") {
                            command.description
                        }
                        if (command.aliases.isNotEmpty()) {
                            field(title = "Aliases:") {
                                command.aliases.joinToString()
                            }
                        }
                        field(title = "Category:") {
                            command.category()
                        }
                        field(title = "Usage:") {
                            command.usage(Immutable.DEFAULT_PREFIX)
                        }
                        if (command.examples(Immutable.DEFAULT_PREFIX).isNotEmpty()) {
                            field(title = "Examples:") {
                                buildString {
                                    for ((example, description) in command.examples(Immutable.DEFAULT_PREFIX)) {
                                        appendln("$example \u2014 $description")
                                    }
                                }
                            }
                        }
                        if (command is AkinatorCommand) {
                            field(title = "Available Languages:") {
                                buildString {
                                    for (language in command.languages.keys) {
                                        appendln("\u2022 ${language.capitalize()}")
                                    }
                                }
                            }
                        }
                        timestamp()
                        footer(event.author.effectiveAvatarUrl) {
                            "Requested by ${event.author.tag}"
                        }
                    }
                    event.channel.sendMessage(embed).queue()
                } else {
                    command(event, emptyArray())
                }
            } else {
                event.sendError("That command doesn't exist!").queue()
            }
        }
    }
}