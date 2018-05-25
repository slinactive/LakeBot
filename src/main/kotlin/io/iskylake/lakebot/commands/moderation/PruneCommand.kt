/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.iskylake.lakebot.commands.moderation

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import java.util.concurrent.TimeUnit

class PruneCommand : Command {
    override val name = "prune"
    override val aliases = listOf("purge", "clear")
    override val usage: (String) -> String = {
        val command = "${super.usage(it)} <count>"
        val dash = '\u2014'
        """$command $dash clears all messages
            |$command @mention $dash clears messages from specified user
            |$command embeds $dash clears embed mогessages
            |$command invites $dash clears messages with invite links
            |$command attachments $dash clears messages with attachments
            |$command bots $dash clears messages from bots
        """.trimMargin()
    }
    override val description = "The command that deletes a specified amount of messages (from 1 to 100)"
    override fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        when {
            !event.selfMember!!.hasPermission(Permission.MESSAGE_MANAGE) -> event.sendError("I don't have permissions for that!").queue()
            !event.member.hasPermission(Permission.MESSAGE_MANAGE) -> event.sendError("You don't have permissions for that!").queue()
            else -> {
                if (args.isNotEmpty()) {
                    if (args[0].isInt) {
                        val count = args[0].toInt()
                        if (args.size > 1) {
                            if (count !in 1..100) {
                                event.sendError("You must specify a number in range from 1 to 100!").queue()
                            } else {
                                if (args[1] matches Regex.DISCORD_USER) {
                                    val user: User? = event.jda.getUserById(args[1].replace(Regex.DISCORD_USER, "\$1"))
                                    if (user === null) {
                                        event.sendError("Couldn't find this user!").queue()
                                    } else {
                                        event.message.delete().queue {
                                            event.textChannel.history.retrievePast(100).queue { historyRaw ->
                                                val history = historyRaw.filter { it.author == user }.take(count)
                                                if (history.isEmpty()) {
                                                    event.sendError("Message history is empty!").queue()
                                                } else {
                                                    if (history.size == 1) {
                                                        history[0].delete().queue {
                                                            event.sendSuccess("Deleted 1 message from ${user.asMention}!").queue {
                                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                    // Ignoring ErrorResponseException
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        event.textChannel.deleteMessages(history).queue {
                                                            event.sendSuccess("Deleted ${history.size} messages from ${user.asMention}!").queue {
                                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                    // Ignoring ErrorResponseException
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val param = args[1].toLowerCase()
                                    when (param) {
                                        "embeds" -> {
                                            event.message.delete().queue {
                                                event.textChannel.history.retrievePast(100).queue { historyRaw ->
                                                    val history = historyRaw.filter { it.embeds.isNotEmpty() }.take(count)
                                                    if (history.isEmpty()) {
                                                        event.sendError("Message history is empty!").queue()
                                                    } else {
                                                        if (history.size == 1) {
                                                            history[0].delete().queue {
                                                                event.sendSuccess("Deleted 1 embed message!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            event.textChannel.deleteMessages(history).queue {
                                                                event.sendSuccess("Deleted ${history.size} embed messages!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        "invites" -> {
                                            event.message.delete().queue {
                                                event.textChannel.history.retrievePast(100).queue { historyRaw ->
                                                    val history = historyRaw.filter { it.invites.isNotEmpty() }.take(count)
                                                    if (history.isEmpty()) {
                                                        event.sendError("Message history is empty!").queue()
                                                    } else {
                                                        if (history.size == 1) {
                                                            history[0].delete().queue {
                                                                event.sendSuccess("Deleted 1 message with invite links!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            event.textChannel.deleteMessages(history).queue {
                                                                event.sendSuccess("Deleted ${history.size} messages with invite links!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        "attachments" -> {
                                            event.message.delete().queue {
                                                event.textChannel.history.retrievePast(100).queue { historyRaw ->
                                                    val history = historyRaw.filter { it.attachments.isNotEmpty() }.take(count)
                                                    if (history.isEmpty()) {
                                                        event.sendError("Message history is empty!").queue()
                                                    } else {
                                                        if (history.size == 1) {
                                                            history[0].delete().queue {
                                                                event.sendSuccess("Deleted 1 message with attachments!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            event.textChannel.deleteMessages(history).queue {
                                                                event.sendSuccess("Deleted ${history.size} messages with attachments!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        "bots" -> {
                                            event.message.delete().queue {
                                                event.textChannel.history.retrievePast(100).queue { historyRaw ->
                                                    val history = historyRaw.filter { it.author.isBot }.take(count)
                                                    if (history.isEmpty()) {
                                                        event.sendError("Message history is empty!").queue()
                                                    } else {
                                                        if (history.size == 1) {
                                                            history[0].delete().queue {
                                                                event.sendSuccess("Deleted 1 message from bot!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            event.textChannel.deleteMessages(history).queue {
                                                                event.sendSuccess("Deleted ${history.size} messages from bots!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        else -> event.sendError("That's an invalid parameter!").queue()
                                    }
                                }
                            }
                        } else {
                            if (count !in 1..100) {
                                event.sendError("You must specify a number in range from 1 to 100!").queue()
                            } else {
                                if (count == 1) {
                                    event.textChannel.history.retrievePast(2).queue { history ->
                                        if (history.isEmpty()) {
                                            event.sendError("Message history is empty!").queue()
                                        } else {
                                            event.textChannel.deleteMessages(history).queue {
                                                event.sendSuccess("Deleted 1 message!").queue {
                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                        // Ignoring ErrorResponseException
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    event.message.delete().complete()
                                    event.textChannel.history.retrievePast(count).queue { history ->
                                        when {
                                            history.isEmpty() -> event.sendError("Message history is empty!").queue()
                                            history.size == 1 -> history[0].delete().queue {
                                                event.sendSuccess("Deleted 1 message!").queue {
                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                        // Ignoring ErrorResponseException
                                                    }
                                                }
                                            }
                                            else -> event.textChannel.deleteMessages(history).queue {
                                                event.sendSuccess("Deleted ${history.size} messages!").queue {
                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                        // Ignoring ErrorResponseException
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        event.sendError("You specified no number!").queue()
                    }
                } else {
                    event.sendError("You specified no count!").queue()
                }
            }
        }
    }
}