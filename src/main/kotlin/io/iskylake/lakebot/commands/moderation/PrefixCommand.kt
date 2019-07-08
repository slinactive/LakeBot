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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class PrefixCommand : Command {
    override val name = "prefix"
    override val aliases = listOf("setprefix")
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <prefix>"
    override val description = "The command that changes command prefix for this server"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (Permission.MANAGE_SERVER in event.member.permissions || event.author.isLBDeveloper) {
        if (args.isNotEmpty()) {
            val newPrefix = args[0].toLowerCase()
            if (newPrefix.length > 5) {
                event.sendError("You can't use that as command prefix!").queue()
            } else {
                event.sendConfirmation("Are you sure you want to change prefix for this server?").await {
                    val confirmation = it.awaitNullableConfirmation(event.author)
                    if (confirmation !== null) {
                        it.delete().queue()
                        if (confirmation) {
                            event.guild.setPrefix(newPrefix)
                            event.sendSuccess("Now prefix is $newPrefix").queue()
                        } else {
                            event.sendSuccess("Successfully canceled!").queue()
                        }
                    } else {
                        event.sendError("Time is up!").queue()
                        it.delete().queue()
                    }
                }
            }
        } else {
            event.sendError("You specified no content!").queue()
        }
    } else {
        event.sendError("You don't have permissions for executing that command!").queue()
    }
}