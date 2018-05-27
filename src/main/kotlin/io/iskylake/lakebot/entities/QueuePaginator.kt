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

package io.iskylake.lakebot.entities

import com.google.common.collect.Lists

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils
import io.iskylake.lakebot.utils.TimeUtils

import kotlinx.coroutines.experimental.async

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.requests.RestAction

import java.util.concurrent.TimeUnit

data class QueuePaginator(
        private val list: List<AudioTrack>,
        private val event: MessageReceivedEvent,
        private val action: (Message) -> Unit = {
            try {
                it.clearReactions().queue()
            } catch (ignored: Exception) {
            }
        }
) {
    companion object {
        const val LEFT = "\u25C0"
        const val STOP = "\u23FA"
        const val RIGHT = "\u25B6"
    }
    val pages = Lists.partition(list, 10)
    fun paginate(ch: MessageChannel = event.channel, pageNum: Int = 1) = accept(ch.sendMessage(this[pageNum]), pageNum)
    private fun accept(rest: RestAction<Message>, pageNum: Int) {
        rest.queue { m ->
            if (pages.size > 1) {
                m.addReaction(LEFT).queue()
                m.addReaction(STOP).queue()
                m.addReaction(RIGHT).queue({ waiter(m, pageNum) }) {
                    waiter(m, pageNum)
                }
            } else {
                action(m)
            }
        }
    }
    private fun waiter(msg: Message, num: Int = 1) {
        async(EventWaiter) {
            val e = EventWaiter.receiveEvent<MessageReactionAddEvent>(1, TimeUnit.MINUTES) {
                it.messageId == msg.id && (LEFT == it.reactionEmote.name || STOP == it.reactionEmote.name || RIGHT == it.reactionEmote.name) && event.author == it.user
            }.await()
            if (e != null) {
                var newPageNum = num
                when (e.reactionEmote.name) {
                    LEFT -> {
                        if (newPageNum > 1) {
                            newPageNum--
                        }
                    }
                    RIGHT -> {
                        if (newPageNum < pages.size) {
                            newPageNum++
                        }
                    }
                    STOP -> {
                        action(msg)
                        return@async
                    }
                }
                try {
                    e.reaction.removeReaction(e.user).queue()
                } catch (ignored: Exception) {
                }
                msg.editMessage(this@QueuePaginator[newPageNum]).queue { msg -> waiter(msg, newPageNum) }
            } else {
                action(msg)
            }
        }
    }
    private operator fun get(num: Int = 1): MessageEmbed {
        var page = num
        if (page >= pages.size) page = pages.size
        else if (page <= 0) page = 1
        val trackPage = pages[page - 1]
        return buildEmbed {
            for (track in trackPage) {
                appendln {
                    "**${list.indexOf(track) + 1}. [${track.info.title}](${track.info.uri}) (${if (track.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(track.duration)})**"
                }
            }
            color {
                Immutable.SUCCESS
            }
            author("LakePlayer") {
                event.selfUser.effectiveAvatarUrl
            }
            field(title = "Total Songs:") {
                "${AudioUtils[event.guild].trackScheduler.queue.size + 1}"
            }
            field(title = "Total Duration:") {
                TimeUtils.asDuration(AudioUtils[event.guild].trackScheduler.queue.map { it.duration }.filter { it != Long.MAX_VALUE }.sum())
            }
            field(title = "Looping:") {
                if (AudioUtils[event.guild].trackScheduler.isLoop) "Enabled" else "Disabled"
            }
            field(title = "Now Playing:") {
                "**[${AudioUtils[event.guild].audioPlayer.playingTrack.info.title}](${AudioUtils[event.guild].audioPlayer.playingTrack.info.uri})** (${TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.position)}/${if (AudioUtils[event.guild].audioPlayer.playingTrack.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.duration)})"
            }
            footer(event.author.effectiveAvatarUrl) {
                "Page $page/${pages.size} | Requested by ${event.author.tag}"
            }
        }
    }
}