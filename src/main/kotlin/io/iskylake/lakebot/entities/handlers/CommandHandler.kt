/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
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

package io.iskylake.lakebot.entities.handlers

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.sendError
import net.dv8tion.jda.core.entities.MessageType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

object CommandHandler {
    val registeredCommands = mutableListOf<Command>()
    private val cooldowns = mutableMapOf<String, OffsetDateTime>()
    operator fun iterator() = registeredCommands.iterator()
    operator fun contains(command: String): Boolean = this[command] !== null && this[command] in registeredCommands
    operator fun contains(command: Command): Boolean = command in registeredCommands
    operator fun get(name: String): Command? {
        val names = registeredCommands.filter { it.name.equals(name, true) }
        val aliases = registeredCommands.filter { name.toLowerCase() in it.aliases }
        return when {
            !names.isEmpty() -> names[0]
            !aliases.isEmpty() -> aliases[0]
            else -> null
        }
    }
    operator fun plusAssign(command: Command) {
        registeredCommands += command
    }
    operator fun minusAssign(command: Command) {
        registeredCommands -= command
    }
    internal operator fun invoke(event: MessageReceivedEvent) {
        if (!event.author.isBot && !event.author.isFake && event.channelType.isGuild && event.message.type == MessageType.DEFAULT) {
            if (event.message.contentRaw.startsWith(Immutable.DEFAULT_PREFIX, true)) {
                val args = event.message.contentRaw.split("\\s".toRegex(), 2)
                val command = this[args[0].toLowerCase().substring(Immutable.DEFAULT_PREFIX.length)]
                if (command !== null) {
                    if (command.isDeveloper && event.author.idLong !in Immutable.DEVELOPERS) {
                        event.sendError("You don't have permissions to execute this command!").queue()
                    } else {
                        if (command.cooldown > 0) {
                            val key = "${command.name}|${event.author.id}"
                            val time = getRemainingCooldown(key)
                            if (time > 0) {
                                val error: String? = getCooldownError(time)
                                if (error !== null) {
                                    event.channel.sendError(error).queue()
                                } else {
                                    if (args.size > 1) {
                                        command(event, args[1].split("\\s+".toRegex()).toTypedArray())
                                    } else {
                                        command(event, emptyArray())
                                    }
                                }
                            } else {
                                applyCooldown(key, command.cooldown)
                                if (args.size > 1) {
                                    command(event, args[1].split("\\s+".toRegex()).toTypedArray())
                                } else {
                                    command(event, emptyArray())
                                }
                            }
                        } else {
                            if (args.size > 1) {
                                command(event, args[1].split("\\s+".toRegex()).toTypedArray())
                            } else {
                                command(event, emptyArray())
                            }
                        }
                    }
                }
            }
        }
    }
    private fun getRemainingCooldown(name: String): Long {
        if (name in cooldowns) {
            val time: Long = OffsetDateTime.now().until(cooldowns[name], ChronoUnit.SECONDS)
            if (time <= 0) {
                cooldowns -= name
                return 0
            }
            return time
        }
        return 0
    }
    private fun applyCooldown(name: String, seconds: Long) {
        cooldowns += name to OffsetDateTime.now().plusSeconds(seconds)
    }
    private fun getCooldownError(time: Long): String? {
        if (time <= 0) {
            return null
        }
        return "Please wait $time ${if (time == 1L) "second" else "seconds"} before launching command again!"
    }
}