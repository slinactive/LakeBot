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

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.utils.ConfigUtils

import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.User

import org.bson.Document

val User.privateChannel: PrivateChannel
    get() = this.openPrivateChannel().complete()
val User.tag: String
    get() = "${this.name}#${this.discriminator}"
val User.lakeBan: Document?
    get() = ConfigUtils.getLakeBan(this)
val User.isLBDeveloper: Boolean
    get() = idLong in Immutable.DEVELOPERS
fun User.putLakeBan(reason: String) = ConfigUtils.putLakeBan(this, reason)
fun User.clearLakeBan() = ConfigUtils.clearLakeBan(this)