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

package io.iskylake.lakebot.commands.developer

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class LakeBanCommand : Command {
    override val name = "lakeban"
    override val description = "The command giving the user LakeBan (after which they can't use LakeBot)"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user> <reason>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (args.size > 1) {
        when {
            args[0] matches Regex.DISCORD_USER -> {
                val user = event.jda.getUserById(args[0].replace(Regex.DISCORD_USER, "\$1"))
                if (user === null) {
                    event.sendError("Couldn't find this user!").queue()
                } else {
                    event.sendConfirmation("Are you sure you want to ban this user?").await {
                        val boolean = it.awaitNullableConfirmation(event.author)
                        if (boolean !== null) {
                            if (boolean) {
                                user.putLakeBan(event.argsRaw!!.split("\\s+".toRegex(), 2)[1])
                                event.sendSuccess("${user.tag} got LakeBan!").queue()
                                it.delete().queue()
                            } else {
                                event.sendSuccess("Successfully canceled!").queue()
                                it.delete().queue()
                            }
                        } else {
                            event.sendError("Time is up!").queue()
                            it.delete().queue()
                        }
                    }
                }
            }
            event.guild.searchMembers(args[0]).isNotEmpty() -> {
                val user = event.guild.searchMembers(args[0])[0].user
                event.sendConfirmation("Are you sure you want to ban this user?").await {
                    val boolean = it.awaitNullableConfirmation(event.author)
                    if (boolean !== null) {
                        if (boolean) {
                            user.putLakeBan(event.argsRaw!!.split("\\s+".toRegex(), 2)[1])
                            event.sendSuccess("${user.tag} got LakeBan!").queue()
                            it.delete().queue()
                        } else {
                            event.sendSuccess("Successfully canceled!").queue()
                            it.delete().queue()
                        }
                    } else {
                        event.sendError("Time is up!").queue()
                        it.delete().queue()
                    }
                }
            }
            else -> event.sendError("Couldn't find this user!").queue()
        }
    } else {
        event.sendError("You didn't specify the reason!").queue()
    }
}