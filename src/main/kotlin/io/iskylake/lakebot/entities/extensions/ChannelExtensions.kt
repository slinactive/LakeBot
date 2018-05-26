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

package io.iskylake.lakebot.entities.extensions

import net.dv8tion.jda.core.entities.MessageChannel

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.entities.EventWaiter

import net.dv8tion.jda.core.entities.User

fun MessageChannel.awaitMessage(user: User) = EventWaiter.awaitMessage(user, this)
fun MessageChannel.sendSuccess(text: String) = this.sendMessage(buildEmbed {
    color { Immutable.SUCCESS }
    author { "Successfully!" }
    description { text }
})
fun MessageChannel.sendError(text: String) = this.sendMessage(buildEmbed {
    color { Immutable.FAILURE }
    author { "Incorrect usage!" }
    description { text }
})
fun MessageChannel.sendConfirmation(text: String) = this.sendMessage(buildEmbed {
    color { Immutable.CONFIRMATION }
    author { "Confirmation" }
    description { text }
})