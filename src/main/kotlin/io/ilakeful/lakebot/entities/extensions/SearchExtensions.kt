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

package io.ilakeful.lakebot.entities.extensions

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

fun JDA.searchUsers(target: String): List<User> {
    val list = userCache.filter {
        getPriority(target, it.name) > 0
    }
    return list.sortedWith(compareBy {
        getPriority(target, it.name)
    }).reversed()
}
fun JDA.searchGuilds(target: String): List<Guild> {
    val list = guildCache.filter {
        getPriority(target, it.name) > 0
    }
    return list.sortedWith(compareBy {
        getPriority(target, it.name)
    }).reversed()
}
fun JDA.getUserByTagSafely(tag: String): User? = try {
    getUserByTag(tag)
} catch (e: Exception) {
    null
}
fun Guild.getMemberByTagSafely(tag: String): Member? = try {
    getMemberByTag(tag)
} catch (e: Exception) {
    null
}
fun Guild.searchMembers(target: String): List<Member> {
    val list = memberCache.filter {
        getPriority(target, it.user.name) > 0
    }
    return list.sortedWith(compareBy {
        getPriority(target, it.user.name)
    }).reversed()
}
fun Guild.searchRoles(target: String): List<Role> {
    val list = roleCache.filter {
        getPriority(target, it.name) > 0
    }
    return list.sortedWith(compareBy {
        getPriority(target, it.name)
    }).reversed()
}
fun Guild.searchTextChannels(target: String): List<TextChannel> {
    val list = textChannelCache.filter {
        getPriority(target, it.name) > 0
    }
    return list.sortedWith(compareBy {
        getPriority(target, it.name)
    }).reversed()
}

suspend fun MessageReceivedEvent.retrieveMembers(
        command: Command? = null,
        query: String? = null,
        mentionMode: Boolean = true,
        massMention: Boolean = true,
        membersLimit: Int = 5,
        applyLimitToMentioned: Boolean = true,
        useAuthorIfNoArguments: Boolean = false,
        predicate: (Member) -> Boolean = { true },
        noArgumentsFailureBlock: suspend () -> Unit = { channel.sendFailure("You haven't specified any arguments!").queue() },
        noMemberFoundBlock: suspend () -> Unit = { channel.sendFailure("LakeBot did not manage to find the required user!").queue() },
        block: suspend (Member) -> Unit
) {
    val arguments = query?.trim() ?: argsRaw
    if (!arguments.isNullOrEmpty()) {
        when {
            if (mentionMode) {
                message.mentionedMembers.any(predicate)
            } else {
                arguments matches Regex.DISCORD_USER && guild.getMemberById(
                        arguments.replace(Regex.DISCORD_USER, "\$1")
                ).let { it !== null && predicate(it) }
            } -> {
                if (mentionMode) {
                    val members by lazy {
                        val mentioned = message.mentionedMembers.filter(predicate)
                        if (applyLimitToMentioned) {
                            mentioned.take(membersLimit)
                        } else {
                            mentioned
                        }
                    }
                    if (massMention) {
                        for (result in members) {
                            block(result)
                        }
                    } else {
                        block(members.first())
                    }
                } else {
                    val result = guild.getMemberById(arguments.replace(Regex.DISCORD_USER, "\$1"))!!
                    block(result)
                }
            }
            guild.getMemberByTagSafely(arguments).let { it !== null && predicate(it) } -> {
                val result = guild.getMemberByTagSafely(arguments)!!
                block(result)
            }
            arguments matches Regex.DISCORD_ID && guild.getMemberById(arguments).let { it !== null && predicate(it) } -> {
                val result = guild.getMemberById(arguments)!!
                block(result)
            }
            guild.searchMembers(arguments).any(predicate) -> {
                val members = guild.searchMembers(arguments).filter(predicate).take(membersLimit)
                if (members.size > 1) {
                    channel.sendEmbed {
                        color { Immutable.SUCCESS }
                        author("Select the User:") { selfUser.effectiveAvatarUrl }
                        for ((index, result) in members.withIndex()) {
                            appendln { "${index + 1}. ${result.user.asTag.escapeDiscordMarkdown()}" }
                        }
                        footer { "Type in \"exit\" to kill the process" }
                    }.await {
                        val process = WaiterProcess(mutableListOf(author), textChannel, command)
                        selectEntity(
                                event = this,
                                message = it,
                                entities = members,
                                addProcess = true,
                                process = process
                        ) { result -> block(result) }
                    }
                } else {
                    block(members.first())
                }
            }
            else -> noMemberFoundBlock()
        }
    } else {
        if (useAuthorIfNoArguments && predicate(member!!)) {
            block(member!!)
        } else {
            noArgumentsFailureBlock()
        }
    }
}
suspend fun MessageReceivedEvent.retrieveRoles(
        command: Command? = null,
        query: String? = null,
        mentionMode: Boolean = true,
        massMention: Boolean = true,
        rolesLimit: Int = 5,
        applyLimitToMentioned: Boolean = true,
        predicate: (Role) -> Boolean = { true },
        noArgumentsFailureBlock: suspend () -> Unit = { channel.sendFailure("You haven't specified any arguments!").queue() },
        noRoleFoundBlock: suspend () -> Unit = { channel.sendFailure("LakeBot did not manage to find the required role!").queue() },
        block: suspend (Role) -> Unit
) {
    val arguments = query?.trim() ?: argsRaw
    if (!arguments.isNullOrEmpty()) {
        when {
            if (mentionMode) {
                message.mentionedRoles.any(predicate)
            } else {
                arguments matches Regex.DISCORD_ROLE && guild.getRoleById(
                        arguments.replace(Regex.DISCORD_ROLE, "\$1")
                ).let { it !== null && predicate(it) }
            } -> {
                if (mentionMode) {
                    val roles by lazy {
                        val mentioned = message.mentionedRoles.filter(predicate)
                        if (applyLimitToMentioned) {
                            mentioned.take(rolesLimit)
                        } else {
                            mentioned
                        }
                    }
                    if (massMention) {
                        for (result in roles) {
                            block(result)
                        }
                    } else {
                        block(roles.first())
                    }
                } else {
                    val result = guild.getRoleById(arguments.replace(Regex.DISCORD_ROLE, "\$1"))!!
                    block(result)
                }
            }
            arguments matches Regex.DISCORD_ID && guild.getRoleById(arguments).let { it !== null && predicate(it) } -> {
                val result = guild.getRoleById(arguments)!!
                block(result)
            }
            guild.searchRoles(arguments).any(predicate) -> {
                val roles = guild.searchRoles(arguments).filter(predicate).take(rolesLimit)
                if (roles.size > 1) {
                    channel.sendEmbed {
                        color { Immutable.SUCCESS }
                        author("Select the Role:") { selfUser.effectiveAvatarUrl }
                        for ((index, result) in roles.withIndex()) {
                            appendln { "${index + 1}. ${result.name.escapeDiscordMarkdown()}" }
                        }
                        footer { "Type in \"exit\" to kill the process" }
                    }.await {
                        val process = WaiterProcess(mutableListOf(author), textChannel, command)
                        selectEntity(
                                event = this,
                                message = it,
                                entities = roles,
                                addProcess = true,
                                process = process
                        ) { result -> block(result) }
                    }
                } else {
                    block(roles.first())
                }
            }
            else -> noRoleFoundBlock()
        }
    } else {
        noArgumentsFailureBlock()
    }
}
internal suspend fun MessageReceivedEvent.retrieveMembersWithIsMassProperty(
        command: Command? = null,
        query: String? = null,
        mentionMode: Boolean = true,
        massMention: Boolean = true,
        membersLimit: Int = 5,
        applyLimitToMentioned: Boolean = true,
        useAuthorIfNoArguments: Boolean = false,
        predicate: (Member) -> Boolean = { true },
        noArgumentsFailureBlock: suspend () -> Unit = { channel.sendFailure("You haven't specified any arguments!").queue() },
        noMemberFoundBlock: suspend () -> Unit = { channel.sendFailure("LakeBot did not manage to find the required user!").queue() },
        block: suspend (Member, isMass: Pair<Boolean, Int>) -> Unit
) {
    val arguments = query?.trim() ?: argsRaw
    if (!arguments.isNullOrEmpty()) {
        when {
            if (mentionMode) {
                message.mentionedMembers.any(predicate)
            } else {
                arguments matches Regex.DISCORD_USER && guild.getMemberById(
                        arguments.replace(Regex.DISCORD_USER, "\$1")
                ).let { it !== null && predicate(it) }
            } -> {
                if (mentionMode) {
                    val members by lazy {
                        val mentioned = message.mentionedMembers.filter(predicate)
                        if (applyLimitToMentioned) {
                            mentioned.take(membersLimit)
                        } else {
                            mentioned
                        }
                    }
                    if (massMention) {
                        for (result in members) {
                            block(result, true to members.size)
                        }
                    } else {
                        block(members.first(), false to 1)
                    }
                } else {
                    val result = guild.getMemberById(arguments.replace(Regex.DISCORD_USER, "\$1"))!!
                    block(result, false to 1)
                }
            }
            guild.getMemberByTagSafely(arguments).let { it !== null && predicate(it) } -> {
                val result = guild.getMemberByTagSafely(arguments)!!
                block(result, false to 1)
            }
            arguments matches Regex.DISCORD_ID && guild.getMemberById(arguments).let { it !== null && predicate(it) } -> {
                val result = guild.getMemberById(arguments)!!
                block(result, false to 1)
            }
            guild.searchMembers(arguments).any(predicate) -> {
                val members = guild.searchMembers(arguments).filter(predicate).take(membersLimit)
                if (members.size > 1) {
                    channel.sendEmbed {
                        color { Immutable.SUCCESS }
                        author("Select the User:") { selfUser.effectiveAvatarUrl }
                        for ((index, result) in members.withIndex()) {
                            appendln { "${index + 1}. ${result.user.asTag.escapeDiscordMarkdown()}" }
                        }
                        footer { "Type in \"exit\" to kill the process" }
                    }.await {
                        val process = WaiterProcess(mutableListOf(author), textChannel, command)
                        selectEntity(
                                event = this,
                                message = it,
                                entities = members,
                                addProcess = true,
                                process = process
                        ) { result -> block(result, false to 1) }
                    }
                } else {
                    block(members.first(), false to 1)
                }
            }
            else -> noMemberFoundBlock()
        }
    } else {
        if (useAuthorIfNoArguments && predicate(member!!)) {
            block(member!!, false to 1)
        } else {
            noArgumentsFailureBlock()
        }
    }
}

fun getPriority(actual: String, expected: String): Int = when {
    expected == actual -> 6
    expected.toLowerCase() == actual.toLowerCase() -> 5
    expected.startsWith(actual) -> 4
    expected.startsWith(actual, true) -> 3
    actual in expected -> 2
    actual.toLowerCase() in expected.toLowerCase() -> 1
    else -> 0
}