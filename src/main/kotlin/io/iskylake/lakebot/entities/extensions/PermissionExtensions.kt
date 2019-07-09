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

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role

val BASIC_PERMISSIONS = listOf(
        Permission.MESSAGE_HISTORY,
        Permission.CREATE_INSTANT_INVITE,
        Permission.MESSAGE_ADD_REACTION,
        Permission.MESSAGE_ATTACH_FILES,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_EXT_EMOJI,
        Permission.MESSAGE_READ,
        Permission.MESSAGE_WRITE,
        Permission.NICKNAME_CHANGE,
        Permission.UNKNOWN,
        Permission.VIEW_CHANNEL,
        Permission.VOICE_CONNECT,
        Permission.VOICE_SPEAK,
        Permission.VOICE_USE_VAD
)
val Member.keyPermissions: List<Permission>
    get() = permissions.filter { it !in BASIC_PERMISSIONS }
val Role.keyPermissions: List<Permission>
    get() = permissions.filter { it !in BASIC_PERMISSIONS }