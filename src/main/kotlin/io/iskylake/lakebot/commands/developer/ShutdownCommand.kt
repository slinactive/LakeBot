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

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*

import kotlin.system.exitProcess

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ShutdownCommand : Command {
    override val name = "shutdown"
    override val aliases = listOf("turnoff", "sd", "turn-off")
    override val description = "The command shutting down LakeBot"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        event.sendConfirmation("Are you sure you want to shutdown ${event.selfUser.name}?").await {
            val confirmation = it.awaitNullableConfirmation(event.author)
            if (confirmation !== null) {
                if (confirmation) {
                    Immutable.LOGGER.info("LakeBot is going offline!")
                    event.channel.sendSuccess("Successfully disconnected!").queue { _ ->
                        it.delete().queue({
                            event.jda.shutdownNow()
                            exitProcess(0)
                        }) {
                            event.jda.shutdownNow()
                            exitProcess(0)
                        }
                    }
                } else {
                    it.delete().queue()
                    event.sendSuccess("Successfully canceled!").queue()
                }
            } else {
                it.delete().queue()
                event.sendError("Time is up!").queue()
            }
        }
    }
}