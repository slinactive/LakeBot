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

package io.ilakeful.lakebot.commands.utils

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.argsRaw
import io.ilakeful.lakebot.entities.extensions.sendFailure
import io.ilakeful.lakebot.utils.ImageUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class QRCommand : Command {
    override val name = "qr"
    override val aliases = listOf("qrcode", "qrgen", "generate-qr", "qr-code")
    override val description = "The command generating an image with QR code from the specified text"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <text>"
    override val cooldown = 2L
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            try {
                event.channel.sendFile(ImageUtils.getQRCode(event.argsRaw!!), "qr.png").queue()
            } catch (e: Exception) {
                event.channel.sendFailure("Something went wrong!").queue()
            }
        } else {
            event.channel.sendFailure("You haven't specified any arguments!").queue()
        }
    }
}