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

package io.ilakeful.lakebot.commands.moderation

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import org.apache.commons.validator.routines.UrlValidator

import java.util.concurrent.TimeUnit

class PruneCommand : Command {
    override val name = "prune"
    override val aliases = listOf("purge", "clearmessages", "clear-messages")
    override val usage: (String) -> String = {
        val command = "${super.usage(it)} <count>"
        val dash = '\u2014'
        """$command $dash clears all messages
            |$command @mention/username#discriminator $dash clears messages from the specified user
            |$command embeds $dash clears embed messages
            |$command invites $dash clears messages containing any invite links
            |$command attachments $dash clears messages containing any attachments
            |$command bots $dash clears messages from bots
            |$command links $dash clears messages containing any links
            |$command mentions $dash clears messages containing any mentions of any users or roles
            |$command contains <content> $dash clears messages containing the specified text
            |$command not <content> $dash clears messages not containing the specified text
            |$command startswith <content> $dash clears messages starting with the specified text
            |$command endswith <content> $dash clears messages ending with the specified text
        """.trimMargin()
    }
    override val description = "The command deleting the specified amount of messages (from 1 to 1000)"
    private fun pruneMessages(
            count: Int,
            response: (Int) -> String,
            event: MessageReceivedEvent,
            predicate: (Message) -> Boolean = { true }
    ) {
        event.message.delete().queue {
            event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                val history = historyRaw.filter(predicate).take(count)
                if (history.isEmpty()) {
                    event.channel.sendFailure("The message history is empty!").queue {
                        it.delete().queueAfter(5, TimeUnit.SECONDS, null) {
                            // Ignoring ErrorResponseException
                        }
                    }
                } else {
                    if (history.size == 1) {
                        history[0].delete().queue {
                            event.channel.sendSuccess(response(1)).queue {
                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) {
                                    // Ignoring ErrorResponseException
                                }
                            }
                        }
                    } else {
                        event.channel.purgeMessages(history)
                        event.channel.sendSuccess(response(history.size)).queue {
                            it.delete().queueAfter(5, TimeUnit.SECONDS, null) {
                                // Ignoring ErrorResponseException
                            }
                        }
                    }
                }
            }
        }
    }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        when (Permission.MESSAGE_MANAGE) {
            !in event.member!!.permissions -> {
                event.channel.sendFailure("You do not have the required permission to manage messages!").queue()
            }
            !in event.selfMember!!.permissions -> {
                event.channel.sendFailure("LakeBot does not have the required permission to manage messages!").queue()
            }
            else -> {
                if (args.isNotEmpty()) {
                    if (args[0].isInt) {
                        val count = args[0].toInt()
                        if (count !in 1..1000) {
                            event.channel.sendFailure("You must specify a number in the range from 1 through 1000!").queue()
                        } else {
                            if (args.size > 1) {
                                val options = setOf(
                                        "embeds",
                                        "invites",
                                        "attachments",
                                        "bots",
                                        "links",
                                        "mentions",
                                        "contains",
                                        "not",
                                        "startswith",
                                        "endswith"
                                )
                                if (args[1] in options) {
                                    when (args[1].toLowerCase()) {
                                        "embeds" -> pruneMessages(
                                                count = count,
                                                response = { "Deleted $it embed message${if (it == 1) "" else "s"}!" },
                                                event = event,
                                                predicate = { it.embeds.isNotEmpty() }
                                        )
                                        "invites" -> pruneMessages(
                                                count = count,
                                                response = { "Deleted $it message${if (it == 1) "" else "s"} containing any invite links!" },
                                                event = event,
                                                predicate = { it.invites.isNotEmpty() }
                                        )
                                        "attachments" -> pruneMessages(
                                                count = count,
                                                response = { "Deleted $it message${if (it == 1) "" else "s"} containing any attachments!" },
                                                event = event,
                                                predicate = { it.attachments.isNotEmpty() }
                                        )
                                        "bots" -> pruneMessages(
                                                count = count,
                                                response = { "Deleted $it message${if (it == 1) "" else "s"} sent by bots!" },
                                                event = event,
                                                predicate = { it.author.isBot }
                                        )
                                        "links" -> pruneMessages(
                                                count = count,
                                                response = { "Deleted $it embed message${if (it == 1) "" else "s"} containing any links!" },
                                                event = event,
                                                predicate = {
                                                    it.contentRaw.split("\\s+".toRegex()).any { el ->
                                                        UrlValidator.getInstance().isValid(el)
                                                    }
                                                }
                                        )
                                        "mentions" -> pruneMessages(
                                                count = count,
                                                response = { "Deleted $it message${if (it == 1) "" else "s"} containing any role or user mentions!" },
                                                event = event,
                                                predicate = { it.mentionedUsers.isNotEmpty() || it.mentionedRoles.isNotEmpty() }
                                        )
                                        "contains" -> {
                                            if (args.size > 2) {
                                                val supposedToBeContained = event.argsRaw!!.split("\\s+".toRegex(), 3).last()
                                                pruneMessages(
                                                        count = count,
                                                        response = { "Deleted $it message${if (it == 1) "" else "s"} containing \"$supposedToBeContained\"!" },
                                                        event = event,
                                                        predicate = { supposedToBeContained in it.contentRaw }
                                                )
                                            } else {
                                                event.channel.sendFailure("The content is not specified!").queue()
                                            }
                                        }
                                        "not" -> {
                                            if (args.size > 2) {
                                                val supposedToNotBeContained = event.argsRaw!!.split("\\s+".toRegex(), 3).last()
                                                pruneMessages(
                                                        count = count,
                                                        response = { "Deleted $it message${if (it == 1) "" else "s"} not containing \"$supposedToNotBeContained\"!" },
                                                        event = event,
                                                        predicate = { supposedToNotBeContained !in it.contentRaw }
                                                )
                                            } else {
                                                event.channel.sendFailure("The content is not specified!").queue()
                                            }
                                        }
                                        "startswith" -> {
                                            if (args.size > 2) {
                                                val supposedToBePrefix = event.argsRaw!!.split("\\s+".toRegex(), 3).last()
                                                pruneMessages(
                                                        count = count,
                                                        response = { "Deleted $it message${if (it == 1) "" else "s"} starting with \"$supposedToBePrefix\"!" },
                                                        event = event,
                                                        predicate = { it.contentRaw.startsWith(supposedToBePrefix) }
                                                )
                                            } else {
                                                event.channel.sendFailure("The content is not specified!").queue()
                                            }
                                        }
                                        "endswith" -> {
                                            if (args.size > 2) {
                                                val supposedToBeSuffix = event.argsRaw!!.split("\\s+".toRegex(), 3).last()
                                                pruneMessages(
                                                        count = count,
                                                        response = { "Deleted $it message${if (it == 1) "" else "s"} ending with \"$supposedToBeSuffix\"!" },
                                                        event = event,
                                                        predicate = { it.contentRaw.startsWith(supposedToBeSuffix) }
                                                )
                                            } else {
                                                event.channel.sendFailure("The content is not specified!").queue()
                                            }
                                        }
                                        else -> event.channel.sendFailure("That is an invalid parameter!").queue()
                                    }
                                } else {
                                    event.retrieveMembers(
                                            query = args[1],
                                            command = this,
                                            mentionMode = false,
                                            massMention = false
                                    ) { member ->
                                        pruneMessages(
                                                count = count,
                                                response = { "Deleted $it message${if (it == 1) "" else "s"} sent by \"${member.user.asTag}\"!" },
                                                event = event,
                                                predicate = { it.author == member.user }
                                        )
                                    }
                                }
                            } else {
                                pruneMessages(
                                        count = count,
                                        response = { "Deleted $it message${if (it == 1) "" else "s"}!" },
                                        event = event
                                )
                            }
                        }
                    } else {
                        event.channel.sendFailure("You haven't specified the amount of message to delete!").queue()
                    }
                } else {
                    event.channel.sendFailure("You haven't specified any arguments!").queue()
                }
            }
        }
    }
}