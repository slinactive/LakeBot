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

package io.iskylake.lakebot.commands.developer

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

import kotlin.system.exitProcess

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class ShutdownCommand : Command {
    override val name = "shutdown"
    override val description = "The command that shutdowns LakeBot"
    override fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        runBlocking {
            Immutable.LOGGER.info("LakeBot is going offline!")
            delay(1000)
            event.message.addReaction(event.jda.getEmoteById(397757496447729664)).queue()
            delay(1000)
            event.jda.shutdownNow()
            delay(1000)
            exitProcess(0)
        }
    }
}