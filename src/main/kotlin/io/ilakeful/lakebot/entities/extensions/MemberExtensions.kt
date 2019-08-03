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

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.VoiceChannel

val Member.joinPosition: Long
    get() = this.guild.memberCache.sortedWith(compareBy { it.timeJoined }).indexOf(this) + 1L
val Member.connectedChannel: VoiceChannel?
    get() = this.voiceState!!.channel
val Member.isConnected: Boolean
    get() = this.voiceState!!.inVoiceChannel() && this.connectedChannel !== null
val Member.joinOrder: String
    get() = this.getJoinOrder(true)
fun Member.getJoinOrder(useLinks: Boolean): String {
    val member = this
    return buildString {
        val joins = member.guild.memberCache.sortedWith(compareBy { it.timeJoined })
        var index = joins.indexOf(member)
        index -= 3
        if (index < 0) {
            index = 0
        }
        if (joins[index] == member) {
            append(if (useLinks) {
                "**[${joins[index].user.name.escapeDiscordMarkdown()}](https://discordapp.com/channels/@me/${joins[index].user.id})**"
            } else {
                "**${joins[index].user.name.escapeDiscordMarkdown()}**"
            })
        } else {
            append(joins[index].user.name.escapeDiscordMarkdown())
        }
        for (i in (index + 1) until (index + 7)) {
            if (i >= joins.size) {
                break
            }
            append(" -> ")
            append(if (joins[i] == member) {
                if (useLinks) {
                    "**[${joins[i].user.name.escapeDiscordMarkdown()}](https://discordapp.com/channels/@me/${joins[i].user.id})**"
                } else {
                    "**${joins[i].user.name.escapeDiscordMarkdown()}**"
                }
            } else {
                joins[i].user.name.escapeDiscordMarkdown()
            })
        }
    }
}