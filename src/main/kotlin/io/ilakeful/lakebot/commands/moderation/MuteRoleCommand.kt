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

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.Permission.*
import net.dv8tion.jda.api.entities.Guild
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
                            MESSAGE_ADD_REACTION,
                            MESSAGE_ATTACH_FILES,
                            MESSAGE_EMBED_LINKS,
                            MESSAGE_MENTION_EVERYONE,
                            MESSAGE_TTS,
                            MESSAGE_WRITE
                    )
                    override.deny = getRaw(denied)
                    override.queue()
                } catch (ignored: Exception) {
                }
            }
            for (channel in guild.voiceChannelCache) {
                try {
                    val override = channel.putPermissionOverride(muteRole)
                    val denied = listOf(
                            PRIORITY_SPEAKER,
                            VOICE_CONNECT,
                            VOICE_SPEAK
                    )
                    override.deny = getRaw(denied)
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
        when (MANAGE_SERVER) {
            !in event.member!!.permissions -> {
                event.channel.sendFailure("You do not have the required permission to manage the server!").queue()
            }
            !in event.selfMember!!.permissions -> {
                event.channel.sendFailure("LakeBot does not have the required permission to manage the server!").queue()
            }
            else -> {
                event.retrieveRoles(
                        command = this,
                        massMention = false,
                        predicate = { !it.isPublicRole },
                        noArgumentsFailureBlock = {
                            if (event.guild.isMuteRoleEnabled) {
                                event.channel.sendConfirmation("Are you sure you want to disable the mute role?").await {
                                    val isWillingToDisable = it.awaitNullableConfirmation(event.author)
                                    it.delete()
                                    if (isWillingToDisable !== null) {
                                        if (isWillingToDisable) {
                                            event.guild.clearMuteRole()
                                            event.channel.sendSuccess("The mute role has been successfully disabled!").queue()
                                        } else {
                                            event.channel.sendSuccess("Successfully canceled!").queue()
                                        }
                                    } else {
                                        event.channel.sendFailure("Time is up!").queue()
                                    }
                                }
                            } else {
                                event.channel.sendFailure("You specified no content!").queue()
                            }
                        }
                ) {
                    event.guild.setMuteRole(it)
                    event.channel.sendSuccess("The mute role has been set!").queue { _ ->
                        denyPermissions(it, event.guild)
                    }
                }
            }
        }
    }
}