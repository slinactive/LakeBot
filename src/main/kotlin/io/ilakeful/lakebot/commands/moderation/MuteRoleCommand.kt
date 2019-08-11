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

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class MuteRoleCommand : Command {
    companion object {
        @JvmStatic
        fun denyPermissions(muteRole: Role, guild: Guild) {
            for (channel in guild.textChannelCache) {
                try {
                    val override = channel.putPermissionOverride(muteRole)
                    val denied = listOf(
                            Permission.MESSAGE_ADD_REACTION,
                            Permission.MESSAGE_ATTACH_FILES,
                            Permission.MESSAGE_EMBED_LINKS,
                            Permission.MESSAGE_MENTION_EVERYONE,
                            Permission.MESSAGE_TTS,
                            Permission.MESSAGE_WRITE
                    )
                    override.deny = Permission.getRaw(denied)
                    override.queue()
                } catch (ignored: Exception) {
                }
            }
            for (channel in guild.voiceChannelCache) {
                try {
                    val override = channel.putPermissionOverride(muteRole)
                    val denied = listOf(
                            Permission.PRIORITY_SPEAKER,
                            Permission.VOICE_CONNECT,
                            Permission.VOICE_SPEAK
                    )
                    override.deny = Permission.getRaw(denied)
                    override.queue()
                } catch (ignored: Exception) {
                }
            }
        }
    }
    override val name = "muterole"
    override val aliases = listOf("mute-role")
    override val description = "The command managing a mute role"
    override val usage = fun(prefix: String) = """
        |${super.usage(prefix)} ${'\u2014'} disables mute role if it iss enabled
        |${super.usage(prefix)} <role> ${'\u2014'} sets mute role""".trimMargin()
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        when (Permission.MANAGE_SERVER) {
            !in event.member!!.permissions -> event.sendFailure("You don't have required permissions!").queue()
            !in event.guild.selfMember.permissions -> event.sendFailure("LakeBot doesn't have required permissions!").queue()
            else -> {
                val arguments = event.argsRaw
                if (arguments !== null) {
                    when {
                        event.message.mentionedRoles.isNotEmpty() -> {
                            event.guild.setMuteRole(event.message.mentionedRoles.first())
                            event.sendSuccess("The mute role has been set!").queue {
                                denyPermissions(event.message.mentionedRoles.first(), event.guild)
                            }
                        }
                        event.guild.searchRoles(arguments).isNotEmpty() -> {
                            val list = event.guild.searchRoles(arguments).take(5)
                            if (list.size > 1) {
                                event.channel.sendMessage(buildEmbed {
                                    color { Immutable.SUCCESS }
                                    author("Select the Role:") { event.selfUser.effectiveAvatarUrl }
                                    for ((index, role) in list.withIndex()) {
                                        appendln { "${index + 1}. ${role.name}" }
                                    }
                                    footer { "Type in \"exit\" to kill the process" }
                                }).await {
                                    val process = WaiterProcess(mutableListOf(event.author), event.textChannel, this)
                                    WAITER_PROCESSES += process
                                    selectRole(event, it, list, process)
                                }
                            } else {
                                event.guild.setMuteRole(list.first())
                                event.sendSuccess("The mute role has been set!").queue {
                                    denyPermissions(list.first(), event.guild)
                                }
                            }
                        }
                        args[0] matches Regex.DISCORD_ID && event.guild.getRoleById(args[0]) !== null -> {
                            val role = event.guild.getRoleById(args[0])!!
                            event.guild.setMuteRole(role)
                            event.sendSuccess("The mute role has been set!").queue {
                                denyPermissions(role, event.guild)
                            }
                        }
                        else -> event.sendFailure("Couldn't find that role!").queue()
                    }
                } else {
                    if (event.guild.isMuteRoleEnabled) {
                        event.sendConfirmation("Are you sure you want to disable mute role?").await {
                            val confirmation = it.awaitNullableConfirmation(event.author)
                            if (confirmation !== null) {
                                if (confirmation) {
                                    it.delete().queue()
                                    event.guild.clearMuteRole()
                                    event.sendSuccess(text = "The mute role has been disabled!").queue()
                                } else {
                                    it.delete().queue()
                                    event.sendSuccess("Process was canceled!").queue()
                                }
                            } else {
                                it.delete().queue()
                                event.sendFailure("Time is up!").queue()
                            }
                        }
                    } else {
                        event.sendFailure("You specified no content!").queue()
                    }
                }
            }
        }
    }
    suspend fun selectRole(event: MessageReceivedEvent, msg: Message, roles: List<Role>, process: WaiterProcess) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..roles.size) {
                    msg.delete().queue()
                    val role = roles[c.toInt() - 1]
                    event.guild.setMuteRole(role)
                    event.sendSuccess("The mute role has been set!").queue()
                    denyPermissions(role, event.guild)
                    WAITER_PROCESSES -= process
                } else {
                    event.sendFailure("Try again!").await { selectRole(event, msg, roles, process) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                WAITER_PROCESSES -= process
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendFailure("Try again!").await { selectRole(event, msg, roles, process) }
            }
        } else {
            msg.delete().queue()
            WAITER_PROCESSES -= process
            event.sendFailure("Time is up!").queue()
        }
    }
}