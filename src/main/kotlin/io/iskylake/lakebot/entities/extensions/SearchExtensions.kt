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

package io.iskylake.lakebot.entities.extensions

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*

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

inline infix fun JDA.searchUsers(crossinline lazy: () -> String): List<User> = this.searchUsers(lazy())
inline infix fun JDA.searchGuilds(crossinline lazy: () -> String): List<Guild> = this.searchGuilds(lazy())
inline infix fun Guild.searchMembers(crossinline lazy: () -> String): List<Member> = this.searchMembers(lazy())
inline infix fun Guild.searchRoles(crossinline lazy: () -> String): List<Role> = this.searchRoles(lazy())
inline infix fun Guild.searchTextChannels(crossinline lazy: () -> String): List<TextChannel> = this.searchTextChannels(lazy())

fun getPriority(actual: String, expected: String): Int = when {
    expected == actual -> 6
    expected.toLowerCase() == actual.toLowerCase() -> 5
    expected.startsWith(actual) -> 4
    expected.startsWith(actual, true) -> 3
    actual in expected -> 2
    actual.toLowerCase() in expected.toLowerCase() -> 1
    else -> 0
}