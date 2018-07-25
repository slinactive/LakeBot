/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.iskylake.lakebot.entities.extensions

import io.iskylake.lakebot.utils.ConfigUtils

import net.dv8tion.jda.core.entities.*

import org.bson.Document

val Guild.prefix: String
    get() = ConfigUtils.getPrefix(this)
val Guild.isPrefixEnabled: Boolean
    get() = ConfigUtils.isPrefixEnabled(this)
fun Guild.setPrefix(prefix: String) = ConfigUtils.setPrefix(prefix, this)
fun Guild.clearPrefix() = ConfigUtils.clearPrefix(this)
val Guild.muteRole: String?
    get() = ConfigUtils.getMuteRole(this)
val Guild.isMuteRoleEnabled: Boolean
    get() = ConfigUtils.isMuteRoleEnabled(this)
fun Guild.setMuteRole(role: Role) = ConfigUtils.setMuteRole(role.id, this)
fun Guild.clearMuteRole() = ConfigUtils.clearMuteRole(this)
val Guild.mutes: List<Document>
    get() = ConfigUtils.getMutes(this)
fun Guild.getMute(user: User) = ConfigUtils.getMute(this, user)
fun Guild.putMute(user: User, mod: User, reason: String, time: Long) = ConfigUtils.putMute(this, user, mod, reason, time)
fun Guild.clearMutes() = ConfigUtils.clearMutes(this)
fun Guild.clearMute(user: User) = ConfigUtils.clearMute(this, user)