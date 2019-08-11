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

import java.time.OffsetDateTime

data class Song(
        val title: String,
        val author: SongAuthor,
        val album: SongAlbum,
        val url: String,
        val previewUrl: String,
        val id: Long,
        val artwork: String?,
        val releaseDate: OffsetDateTime,
        val genre: String,
        val number: Int,
        val duration: Long,
        val isExplicit: Boolean
)
class SongBuilder {
    lateinit var titleProperty: String
    lateinit var authorProperty: SongAuthor
    lateinit var albumProperty: SongAlbum
    lateinit var urlProperty: String
    lateinit var preview: String
    lateinit var dateProperty: OffsetDateTime
    lateinit var genreProperty: String
    var idProperty: Long = 0
    var artworkProperty: String? = null
    var numberProperty: Int = 0
    var durationProperty: Long = 0
    var isExplicit: Boolean = false
    inline infix fun title(lazyTitle: () -> String) {
        titleProperty = lazyTitle()
    }
    inline infix fun album(block: SongAlbumBuilder.() -> Unit) {
        albumProperty = SongAlbumBuilder().apply(block)()
    }
    inline infix fun author(block: SongAuthorBuilder.() -> Unit) {
        authorProperty = SongAuthorBuilder().apply(block)()
    }
    inline infix fun url(lazyUrl: () -> String) {
        urlProperty = lazyUrl()
    }
    inline infix fun previewUrl(lazyPreview: () -> String) {
        preview = lazyPreview()
    }
    inline infix fun releaseDate(lazyDate: () -> OffsetDateTime) {
        dateProperty = lazyDate()
    }
    inline infix fun genre(lazyGenre: () -> String) {
        genreProperty = lazyGenre()
    }
    inline infix fun artwork(lazyArtwork: () -> String?) {
        artworkProperty = lazyArtwork()
    }
    inline infix fun id(lazyId: () -> Long) {
        idProperty = lazyId()
    }
    inline infix fun number(lazyNumber: () -> Int) {
        numberProperty = lazyNumber()
    }
    inline infix fun duration(lazyDuration: () -> Long) {
        durationProperty = lazyDuration()
    }
    inline infix fun explicit(lazyIsExplicit: () -> Boolean) {
        isExplicit = lazyIsExplicit()
    }
    operator fun invoke() = if (
            ::titleProperty.isInitialized
            && ::albumProperty.isInitialized
            && ::authorProperty.isInitialized
            && ::urlProperty.isInitialized
            && ::preview.isInitialized
            && ::dateProperty.isInitialized
            && ::genreProperty.isInitialized
            && numberProperty > 0
            && idProperty > 0
            && durationProperty > 0
    ) {
        Song(
                titleProperty,
                authorProperty,
                albumProperty,
                urlProperty,
                preview,
                idProperty,
                artworkProperty,
                dateProperty,
                genreProperty,
                numberProperty,
                durationProperty,
                isExplicit
        )
    } else throw IllegalStateException("Some important properties were not initialized!")
}
inline fun buildSong(block: SongBuilder.() -> Unit) = SongBuilder().apply(block)()