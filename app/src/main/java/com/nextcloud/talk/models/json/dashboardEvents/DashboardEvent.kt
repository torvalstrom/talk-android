/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.models.json.dashboardEvents

import android.os.Parcelable
import com.bluelinelabs.logansquare.annotation.JsonField
import com.bluelinelabs.logansquare.annotation.JsonObject
import kotlinx.parcelize.Parcelize

/**
 * One entry of GET /ocs/v2.php/apps/spreed/api/v4/dashboard/events: a calendar event in the next
 * seven days whose location is a conversation on this server. This is what the Talk web dashboard
 * lists as upcoming meetings (Talk 21+).
 */
@Parcelize
@JsonObject
data class DashboardEvent(
    @JsonField(name = ["eventName"])
    var eventName: String?,
    @JsonField(name = ["eventDescription"])
    var eventDescription: String?,
    @JsonField(name = ["eventLink"])
    var eventLink: String?,
    @JsonField(name = ["start"])
    var start: Long?,
    @JsonField(name = ["end"])
    var end: Long?,
    @JsonField(name = ["roomToken"])
    var roomToken: String?,
    @JsonField(name = ["roomName"])
    var roomName: String?,
    @JsonField(name = ["roomDisplayName"])
    var roomDisplayName: String?,
    @JsonField(name = ["roomType"])
    var roomType: Int?,
    @JsonField(name = ["roomActiveSince"])
    var roomActiveSince: Long?,
    @JsonField(name = ["invited"])
    var invited: Int?,
    @JsonField(name = ["accepted"])
    var accepted: Int?
) : Parcelable {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(null, null, null, null, null, null, null, null, null, null, null, null)
}
