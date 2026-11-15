/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.conversationlist.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nextcloud.talk.R
import com.nextcloud.talk.models.json.dashboardEvents.DashboardEvent
import com.nextcloud.talk.utils.DateUtils
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private const val COLLAPSED_COUNT = 3

/**
 * "Upcoming meetings" above the conversation list: the calendar events of the next seven days
 * that point at a conversation on this server (what the web dashboard shows). Hidden when there
 * are none. Collapsed to three rows with a "show all" toggle so a busy week does not push the
 * conversations off the screen.
 */
@Composable
fun UpcomingMeetingsCard(
    meetings: List<DashboardEvent>,
    visible: Boolean,
    onMeetingClick: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    AnimatedVisibility(visible = visible && meetings.isNotEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_event_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.nc_upcoming_meetings),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val shown = if (expanded) meetings else meetings.take(COLLAPSED_COUNT)
                shown.forEach { meeting ->
                    UpcomingMeetingRow(meeting = meeting, onClick = { onMeetingClick(meeting) })
                }
                if (meetings.size > COLLAPSED_COUNT) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (expanded) {
                                stringResource(R.string.nc_upcoming_meetings_show_less)
                            } else {
                                stringResource(R.string.nc_upcoming_meetings_show_all, meetings.size)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingMeetingRow(meeting: DashboardEvent, onClick: () -> Unit) {
    val context = LocalContext.current
    val timeString = remember(meeting.start) {
        meeting.start?.let { start ->
            val startDateTime = Instant.ofEpochSecond(start).atZone(ZoneId.systemDefault())
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            DateUtils(context).getStringForMeetingStartDateTime(startDateTime, now)
        }.orEmpty()
    }
    val ongoing = meeting.roomActiveSince != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeString,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = if (ongoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (ongoing) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.nc_ongoing_meeting),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = meeting.eventName.orEmpty().ifBlank { meeting.roomDisplayName.orEmpty() },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val room = meeting.roomDisplayName.orEmpty()
        if (room.isNotBlank() && room != meeting.eventName) {
            Text(
                text = room,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
