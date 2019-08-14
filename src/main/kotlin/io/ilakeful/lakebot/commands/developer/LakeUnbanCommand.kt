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
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <member>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = event.retrieveMembers(
            command = this,
            predicate = { it.user.lakeBan !== null }
    ) {
        it.user.clearLakeBan()
        event.channel.sendSuccess("${it.user.asTag} has been successfully released from LakeBan!").queue()
    }
}