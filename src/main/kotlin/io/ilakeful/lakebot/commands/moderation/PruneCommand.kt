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
import net.dv8tion.jda.api.entities.User
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
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        when {
            !event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE) -> event.sendFailure("I don't have permissions for that!").queue()
            !event.member!!.hasPermission(Permission.MESSAGE_MANAGE) -> event.sendFailure("You don't have permissions for that!").queue()
            else -> {
                if (args.isNotEmpty()) {
                    if (args[0].isInt) {
                        val count = args[0].toInt()
                        if (args.size > 1) {
                            if (count !in 1..1000) {
                                event.sendFailure("You must specify a number in range from 1 to 1000!").queue()
                            } else {
                                when {
                                    args[1] matches Regex.DISCORD_USER -> {
                                        val user: User? = event.jda.getUserById(args[1].replace(Regex.DISCORD_USER, "\$1"))
                                        if (user === null) {
                                            event.sendFailure("Couldn't find this user!").queue()
                                        } else {
                                            event.message.delete().queue {
                                                event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                    val history = historyRaw.filter { it.author == user }.take(count)
                                                    if (history.isEmpty()) {
                                                        event.sendFailure("Message history is empty!").queue {
                                                            it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                // Ignoring ErrorResponseException
                                                            }
                                                        }
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
                                                            event.channel.purgeMessages(history)
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
                                    event.guild.getMemberByTagSafely(event.argsRaw!!.removePrefix("${args[0]} ")) !== null -> {
                                        val user = event.jda.getUserByTagSafely(event.argsRaw!!.removePrefix("${args[0]} "))!!
                                        event.message.delete().queue {
                                            event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                val history = historyRaw.filter { it.author == user }.take(count)
                                                if (history.isEmpty()) {
                                                    event.channel.sendFailure("Message history is empty!").queue {
                                                        it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                            // Ignoring ErrorResponseException
                                                        }
                                                    }
                                                } else {
                                                    if (history.size == 1) {
                                                        history[0].delete().queue {
                                                            event.channel.sendSuccess("Deleted 1 message from ${user.asMention}!").queue {
                                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                    // Ignoring ErrorResponseException
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        event.channel.purgeMessages(history)
                                                        event.channel.sendSuccess("Deleted ${history.size} messages from ${user.asMention}!").queue {
                                                            it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                // Ignoring ErrorResponseException
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        when (args[1].toLowerCase()) {
                                            "embeds" -> {
                                                event.message.delete().queue {
                                                    event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                        val history = historyRaw.filter { it.embeds.isNotEmpty() }.take(count)
                                                        if (history.isEmpty()) {
                                                            event.sendFailure("Message history is empty!").queue {
                                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                    // Ignoring ErrorResponseException
                                                                }
                                                            }
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
                                                                event.channel.purgeMessages(history)
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
                                            "invites" -> {
                                                event.message.delete().queue {
                                                    event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                        val history = historyRaw.filter { it.invites.isNotEmpty() }.take(count)
                                                        if (history.isEmpty()) {
                                                            event.sendFailure("Message history is empty!").queue {
                                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                    // Ignoring ErrorResponseException
                                                                }
                                                            }
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
                                                                event.channel.purgeMessages(history)
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
                                            "attachments" -> {
                                                event.message.delete().queue {
                                                    event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                        val history = historyRaw.filter { it.attachments.isNotEmpty() }.take(count)
                                                        if (history.isEmpty()) {
                                                            event.sendFailure("Message history is empty!").queue {
                                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                    // Ignoring ErrorResponseException
                                                                }
                                                            }
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
                                                                event.channel.purgeMessages(history)
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
                                            "bots" -> {
                                                event.message.delete().queue {
                                                    event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                        val history = historyRaw.filter { it.author.isBot }.take(count)
                                                        if (history.isEmpty()) {
                                                            event.sendFailure("Message history is empty!").queue {
                                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                    // Ignoring ErrorResponseException
                                                                }
                                                            }
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
                                                                event.channel.purgeMessages(history)
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
                                            "links" -> {
                                                event.message.delete().queue {
                                                    event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                        val history = historyRaw.filter { it.contentRaw.split("\\s+".toRegex()).filter { e ->
                                                            UrlValidator.getInstance().isValid(e)
                                                        }.isNotEmpty() }.take(count)
                                                        if (history.isEmpty()) {
                                                            event.channel.sendFailure("Message history is empty!").queue {
                                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                    // Ignoring ErrorResponseException
                                                                }
                                                            }
                                                        } else {
                                                            if (history.size == 1) {
                                                                history[0].delete().queue {
                                                                    event.channel.sendSuccess("Deleted 1 message with links!").queue {
                                                                        it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                            // Ignoring ErrorResponseException
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                event.channel.purgeMessages(history)
                                                                event.channel.sendSuccess("Deleted ${history.size} messages with links!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            "mentions" -> {
                                                event.message.delete().queue {
                                                    event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                        val history = historyRaw.filter { it.mentionedUsers.isNotEmpty() || it.mentionedRoles.isNotEmpty() }.take(count)
                                                        if (history.isEmpty()) {
                                                            event.channel.sendFailure("Message history is empty!").queue {
                                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                    // Ignoring ErrorResponseException
                                                                }
                                                            }
                                                        } else {
                                                            if (history.size == 1) {
                                                                history[0].delete().queue {
                                                                    event.channel.sendSuccess("Deleted 1 message with mentions!").queue {
                                                                        it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                            // Ignoring ErrorResponseException
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                event.channel.purgeMessages(history)
                                                                event.channel.sendSuccess("Deleted ${history.size} messages with mentions!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            "contains" -> {
                                                if (args.size > 2) {
                                                    val supposedToBeContained = event.argsRaw!!.split("\\s+".toRegex(), 3).last()
                                                    event.message.delete().queue {
                                                        event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                            val history = historyRaw.filter { supposedToBeContained in it.contentRaw }.take(count)
                                                            if (history.isEmpty()) {
                                                                event.channel.sendFailure("Message history is empty!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            } else {
                                                                if (history.size == 1) {
                                                                    history[0].delete().queue {
                                                                        event.channel.sendSuccess("Deleted 1 message containing \"$supposedToBeContained\"!").queue {
                                                                            it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                                // Ignoring ErrorResponseException
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    event.channel.purgeMessages(history)
                                                                    event.channel.sendSuccess("Deleted ${history.size} messages containing \"$supposedToBeContained\"!").queue {
                                                                        it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                            // Ignoring ErrorResponseException
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    event.channel.sendFailure("The content is not specified!").queue()
                                                }
                                            }
                                            "not" -> {
                                                if (args.size > 2) {
                                                    val supposedToNotBeContained = event.argsRaw!!.split("\\s+".toRegex(), 3).last()
                                                    event.message.delete().queue {
                                                        event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                            val history = historyRaw.filter { supposedToNotBeContained !in it.contentRaw }.take(count)
                                                            if (history.isEmpty()) {
                                                                event.channel.sendFailure("Message history is empty!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            } else {
                                                                if (history.size == 1) {
                                                                    history[0].delete().queue {
                                                                        event.channel.sendSuccess("Deleted 1 message not containing \"$supposedToNotBeContained\"!").queue {
                                                                            it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                                // Ignoring ErrorResponseException
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    event.channel.purgeMessages(history)
                                                                    event.channel.sendSuccess("Deleted ${history.size} messages not containing \"$supposedToNotBeContained\"!").queue {
                                                                        it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                            // Ignoring ErrorResponseException
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    event.channel.sendFailure("The content is not specified!").queue()
                                                }
                                            }
                                            "startswith" -> {
                                                if (args.size > 2) {
                                                    val supposedToBePrefix = event.argsRaw!!.split("\\s+".toRegex(), 3).last()
                                                    event.message.delete().queue {
                                                        event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                            val history = historyRaw.filter { it.contentRaw.startsWith(supposedToBePrefix) }.take(count)
                                                            if (history.isEmpty()) {
                                                                event.channel.sendFailure("Message history is empty!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            } else {
                                                                if (history.size == 1) {
                                                                    history[0].delete().queue {
                                                                        event.channel.sendSuccess("Deleted 1 message starting with \"$supposedToBePrefix\"!").queue {
                                                                            it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                                // Ignoring ErrorResponseException
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    event.channel.purgeMessages(history)
                                                                    event.channel.sendSuccess("Deleted ${history.size} messages starting with \"$supposedToBePrefix\"!").queue {
                                                                        it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                            // Ignoring ErrorResponseException
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    event.channel.sendFailure("The content is not specified!").queue()
                                                }
                                            }
                                            "endswith" -> {
                                                if (args.size > 2) {
                                                    val supposedToBeSuffix = event.argsRaw!!.split("\\s+".toRegex(), 3).last()
                                                    event.message.delete().queue {
                                                        event.channel.iterableHistory.takeAsync(1000).thenAccept { historyRaw ->
                                                            val history = historyRaw.filter { it.contentRaw.endsWith(supposedToBeSuffix) }.take(count)
                                                            if (history.isEmpty()) {
                                                                event.channel.sendFailure("Message history is empty!").queue {
                                                                    it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                        // Ignoring ErrorResponseException
                                                                    }
                                                                }
                                                            } else {
                                                                if (history.size == 1) {
                                                                    history[0].delete().queue {
                                                                        event.channel.sendSuccess("Deleted 1 message ending with \"$supposedToBeSuffix\"!").queue {
                                                                            it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                                // Ignoring ErrorResponseException
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    event.channel.purgeMessages(history)
                                                                    event.channel.sendSuccess("Deleted ${history.size} messages ending with \"$supposedToBeSuffix\"!").queue {
                                                                        it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                                            // Ignoring ErrorResponseException
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    event.channel.sendFailure("The content is not specified!").queue()
                                                }
                                            }
                                            else -> event.sendFailure("That's an invalid parameter!").queue()
                                        }
                                    }
                                }
                            }
                        } else {
                            if (count !in 1..1000) {
                                event.sendFailure("You must specify a number in range from 1 to 1000!").queue()
                            } else {
                                event.message.delete().queue {
                                    event.channel.iterableHistory.takeAsync(count).thenAccept { history ->
                                        if (history.isEmpty()) {
                                            event.sendFailure("Message history is empty!").queue {
                                                it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                    // Ignoring ErrorResponseException
                                                }
                                            }
                                        } else {
                                            if (history.size == 1) {
                                                history[0].delete().queue {
                                                    event.sendSuccess("Deleted 1 message!").queue {
                                                        it.delete().queueAfter(5, TimeUnit.SECONDS, null) { _ ->
                                                            // Ignoring ErrorResponseException
                                                        }
                                                    }
                                                }
                                            } else {
                                                event.channel.purgeMessages(history)
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
                        event.sendFailure("You specified no number!").queue()
                    }
                } else {
                    event.sendFailure("You specified no count!").queue()
                }
            }
        }
    }
}