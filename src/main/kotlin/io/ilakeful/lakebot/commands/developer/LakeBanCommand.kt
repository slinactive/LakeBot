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

class LakeBanCommand : Command {
    override val name = "lakeban"
    override val description = "The command giving the user LakeBan (after which they cannot use LakeBot any longer)"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user> <reason>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (args.size > 1) {
        event.retrieveMembers(
                command = this,
                query = args.first(),
                massMention = false,
                predicate = { !it.user.isLBDeveloper && !it.user.isBot }
        ) { member ->
            val user = member.user
            event.channel.sendConfirmation("Are you sure you want to give LakeBan to ${user.asTag}?").await {
                val isWillingToBan = it.awaitNullableConfirmation(event.author)
                if (isWillingToBan !== null) {
                    if (isWillingToBan) {
                        user.putLakeBan(event.argsRaw?.split("\\s+".toRegex(), 2)?.last() ?: "No reason provided")
                        event.channel.sendSuccess("${user.asTag} has successfully gotten LakeBan!").queue()
                        it.delete().queue()
                    } else {
                        event.channel.sendSuccess("Successfully canceled!").queue()
                        it.delete().queue()
                    }
                } else {
                    event.channel.sendFailure("Time is up!")
                }
            }
        }
    } else {
        event.channel.sendFailure("The command has been used in a wrong way!").queue()
    }
}