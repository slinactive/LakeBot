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

package io.ilakeful.lakebot.commands.`fun`

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.ImageUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import java.net.URL

class InvertCommand : Command {
    override val name = "invert"
    override val description = "The command inverting colors of the attached image or the avatar of the specified user"
    override val aliases = listOf("negative", "negate")
    override val cooldown = 3L
    override val usage: (String) -> String = {
        val command = super.usage(it)
        """$command (without attachments) -> inverts your avatar
            |$command -> inverts an attached image
            |$command <user mention> -> inverts an avatar of specified user
            |$command <url> -> inverts an image from the link""".trimMargin()
    }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (args.isNotEmpty()) {
            try {
                ImageUtils.urlToBytes(URL(event.argsRaw!!)).let {
                    event.channel.sendFile(ImageUtils.imageToBytes(ImageUtils.getInvertedImage(it)), "inverted.png").queue()
                }
            } catch (t: Throwable) {
                when {
                    event.message.mentionedMembers.isNotEmpty() -> {
                        val avatar = "${event.message.mentionedMembers[0].user.effectiveAvatarUrl}?size=2048"
                        event.channel.sendFile(ImageUtils.imageToBytes(ImageUtils.getInvertedImage(avatar)), "inverted.png").queue()
                    }
                    event.guild.getMemberByTagSafely(event.argsRaw!!) !== null -> {
                        val avatar = "${event.jda.getUserByTagSafely(event.argsRaw!!)!!.effectiveAvatarUrl}?size=2048"
                        event.channel.sendFile(ImageUtils.imageToBytes(ImageUtils.getInvertedImage(avatar)), "inverted.png").queue()
                    }
                    event.guild.searchMembers(event.argsRaw!!).isNotEmpty() -> {
                        val avatar = "${event.guild.searchMembers(event.argsRaw!!)[0].user.effectiveAvatarUrl}?size=2048"
                        event.channel.sendFile(ImageUtils.imageToBytes(ImageUtils.getInvertedImage(avatar)), "inverted.png").queue()
                    }
                    else -> event.sendFailure("The user cannot be found!").queue()
                }
            }
        } else {
            if (event.message.attachments.isNotEmpty()) {
                val attachment = event.message.attachments[0]
                if (attachment.isImage) {
                    val att = "${attachment.url}?size=2048"
                    event.channel.sendFile(ImageUtils.imageToBytes(ImageUtils.getInvertedImage(att)), "inverted.png").queue()
                } else {
                    event.message.addReaction("\u274C").queue()
                }
            } else {
                val avatar = "${event.author.effectiveAvatarUrl}?size=2048"
                event.channel.sendFile(ImageUtils.imageToBytes(ImageUtils.getInvertedImage(avatar)), "inverted.png").queue()
            }
        }
    }
}