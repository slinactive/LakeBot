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

package io.ilakeful.lakebot.commands.general

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.commands.CommandCategory
import io.ilakeful.lakebot.commands.`fun`.AkinatorCommand
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.entities.handlers.CommandHandler
import io.ilakeful.lakebot.utils.TimeUtils

import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class HelpCommand : Command {
    override val name = "help"
    override val description = "N/A"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (args.isEmpty()) {
            val embed = buildEmbed {
                color { Immutable.SUCCESS }
                author("${event.selfUser.name} Help:") { event.selfUser.effectiveAvatarUrl }
                description {
                    "**[Invite Link](https://discordapp.com/oauth2/authorize?client_id=${event.selfUser.id}&permissions=${Immutable.PERMISSIONS}&scope=bot)** \u2022 **[GitHub Repository](${Immutable.GITHUB_REPOSITORY})**"
                }
                timestamp()
                footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.asTag}" }
                val categories = mutableMapOf<CommandCategory, MutableList<Command>>()
                for (command in CommandHandler.registeredCommands) {
                    val category = command.category
                    val edit: MutableList<Command> = categories[category] ?: mutableListOf()
                    edit += command
                    categories += category to edit
                }
                val commandFields = mutableListOf<MessageEmbed.Field>()
                for ((category, commands) in categories) {
                    val list = commands.map { it.name }.sorted().joinToString()
                    commandFields += MessageEmbed.Field("${category().capitalize()} Commands:", list, false)
                }
                for (field in commandFields.sortedBy { it.name }) {
                    field { field }
                }
            }
            val emote = event.jda.getEmoteById(397757496447729664)
            if (emote !== null) {
                event.message.addReaction(emote).queue()
            } else {
                event.message.addReaction("\u2705").queue()
            }
            event.author.openPrivateChannel().queue {
                it.sendMessage(embed).queue(null) {
                    event.channel.sendMessage(embed).queue()
                }
            }
        } else {
            val command = CommandHandler[args[0]]
            if (command !== null) {
                if (command !is HelpCommand) {
                    val embed = buildEmbed {
                        color { Immutable.SUCCESS }
                        author("${event.guild.prefix}${command.name}") { event.selfUser.effectiveAvatarUrl }
                        field(title = "Description:") { command.description }
                        if (command.aliases.isNotEmpty()) {
                            field(title = "Aliases:") {
                                command.aliases.sorted().joinToString()
                            }
                        }
                        field(title = "Category:") { command.category() }
                        if (command.cooldown > 0) {
                            field(title = "Cooldown:") { TimeUtils.asText(command.cooldown * 1000) }
                        }
                        field(title = "Usage:") { command.usage(event.guild.prefix) }
                        if (command.examples(event.guild.prefix).isNotEmpty()) {
                            field(title = "Examples:") {
                                buildString {
                                    for ((example, description) in command.examples(event.guild.prefix)) {
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
                        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.asTag}" }
                    }
                    event.channel.sendMessage(embed).queue()
                } else {
                    command(event, emptyArray())
                }
            } else {
                event.channel.sendFailure("The command does not exist!").queue()
            }
        }
    }
}