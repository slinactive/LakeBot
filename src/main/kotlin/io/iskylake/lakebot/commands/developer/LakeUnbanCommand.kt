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

class LakeUnbanCommand : Command {
    override val name = "lakeunban"
    override val description = "The command that gives user LakeBan (after which they can't use LakeBot)"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (args.isNotEmpty()) {
        when {
            event.message.mentionedUsers.isNotEmpty() -> {
                val user = event.message.mentionedUsers[0]
                if (user.lakeBan === null) {
                    event.sendError("This user isn't banned!").queue()
                } else {
                    user.clearLakeBan()
                    event.sendSuccess("User was unbanned successfully!").queue()
                }
            }
            event.guild.searchMembers(event.argsRaw!!).isNotEmpty() -> {
                val user = event.guild.searchMembers(event.argsRaw!!)[0].user
                if (user.lakeBan === null) {
                    event.sendError("This user isn't banned!").queue()
                } else {
                    user.clearLakeBan()
                    event.sendSuccess("User was unbanned successfully!").queue()
                }
            }
            else -> event.sendError("Couldn't find this user!").queue()
        }
    } else {
        event.sendError("You specified no content!").queue()
    }
}