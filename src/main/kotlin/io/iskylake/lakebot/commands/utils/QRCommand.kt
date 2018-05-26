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

package io.iskylake.lakebot.commands.utils

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.argsRaw
import io.iskylake.lakebot.entities.extensions.sendError
import io.iskylake.lakebot.utils.ImageUtils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class QRCommand : Command {
    override val name = "qr"
    override val aliases = listOf("qrcode", "makeqr")
    override val description = "The command that creates an image with QR code from the given text"
    override val usage: (String) -> String = { "${super.usage(it)} <text>" }
    override fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            try {
                event.channel.sendFile(ImageUtils.getQRCode(event.argsRaw!!), "qr.png").queue()
            } catch (e: Exception) {
                event.sendError("Something went wrong!").queue()
            }
        } else {
            event.sendError("You specified no content!").queue()
        }
    }
}