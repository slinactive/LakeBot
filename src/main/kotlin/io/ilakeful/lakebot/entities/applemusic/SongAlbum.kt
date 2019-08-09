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
 *  limitations under the License.
 */

package io.ilakeful.lakebot.entities.applemusic

data class SongAlbum(
        val title: String,
        val count: Int,
        val id: Long,
        val url: String,
        val isExplicit: Boolean,
        val titleCensored: String
)
class SongAlbumBuilder {
    lateinit var titleProperty: String
    lateinit var titleCensoredProperty: String
    lateinit var urlProperty: String
    var idProperty: Long = 0
    var countProperty: Int = 0
    var isExplicit: Boolean = false
    inline infix fun title(lazyTitle: () -> String) {
        titleProperty = lazyTitle()
    }
    inline infix fun titleCensored(lazyTitleCensored: () -> String) {
        titleCensoredProperty = lazyTitleCensored()
    }
    inline infix fun url(lazyUrl: () -> String) {
        urlProperty = lazyUrl()
    }
    inline infix fun id(lazyId: () -> Long) {
        idProperty = lazyId()
    }
    inline infix fun count(lazyCount: () -> Int) {
        countProperty = lazyCount()
    }
    inline infix fun explicit(lazyIsExplicit: () -> Boolean) {
        isExplicit = lazyIsExplicit()
    }
    operator fun invoke() = if (
            ::titleProperty.isInitialized
            && ::titleCensoredProperty.isInitialized
            && ::urlProperty.isInitialized
            && idProperty > 0
            && countProperty > 0
    ) {
        SongAlbum(titleProperty, countProperty, idProperty, urlProperty, isExplicit, titleCensoredProperty)
    } else throw IllegalStateException("Some important properties were not initialized!")
}
inline fun songAlbum(block: SongAlbumBuilder.() -> Unit) = SongAlbumBuilder().apply(block)()