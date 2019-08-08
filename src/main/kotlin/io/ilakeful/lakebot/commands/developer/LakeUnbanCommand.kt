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

package io.ilakeful.lakebot.commands.developer

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class LakeUnbanCommand : Command {
    override val name = "lakeunban"
    override val description = "The command lifting LakeBan against the user"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (args.isNotEmpty()) {
        when {
            event.message.mentionedUsers.isNotEmpty() -> {
                val user = event.message.mentionedUsers[0]
                if (user.lakeBan === null) {
                    event.sendFailure("This user isn't banned!").queue()
                } else {
                    user.clearLakeBan()
                    event.sendSuccess("User was unbanned successfully!").queue()
                }
            }
            event.jda.getUserByTagSafely(event.argsRaw!!) !== null -> {
                val user = event.jda.getUserByTagSafely(event.argsRaw!!)!!
                if (user.lakeBan === null) {
                    event.sendFailure("This user isn't banned!").queue()
                } else {
                    user.clearLakeBan()
                    event.sendSuccess("User was unbanned successfully!").queue()
                }
            }
            event.guild.searchMembers(event.argsRaw!!).isNotEmpty() -> {
                val user = event.guild.searchMembers(event.argsRaw!!)[0].user
                if (user.lakeBan === null) {
                    event.sendFailure("This user isn't banned!").queue()
                } else {
                    user.clearLakeBan()
                    event.sendSuccess("User was unbanned successfully!").queue()
                }
            }
            else -> event.sendFailure("Couldn't find this user!").queue()
        }
    } else {
        event.sendFailure("You specified no content!").queue()
    }
}