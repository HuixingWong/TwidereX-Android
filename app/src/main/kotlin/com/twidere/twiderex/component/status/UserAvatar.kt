/*
 *  Twidere X
 *
 *  Copyright (C) 2020 Tlaster <tlaster@outlook.com>
 * 
 *  This file is part of Twidere X.
 * 
 *  Twidere X is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Twidere X is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Twidere X. If not, see <http://www.gnu.org/licenses/>.
 */
package com.twidere.twiderex.component.status

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import com.twidere.twiderex.component.foundation.NetworkImage
import com.twidere.twiderex.component.navigation.AmbientNavigator
import com.twidere.twiderex.model.ui.UiUser
import com.twidere.twiderex.preferences.AmbientDisplayPreferences
import com.twidere.twiderex.preferences.proto.DisplayPreferences
import com.twidere.twiderex.ui.profileImageSize

@Composable
fun UserAvatar(
    user: UiUser,
    size: Dp = profileImageSize
) {
    val navigator = AmbientNavigator.current
    Box(
        modifier = Modifier
            .withAvatarClip()
            .clipToBounds()
    ) {
        NetworkImage(
            url = user.profileImage,
            modifier = Modifier
                .clickable(
                    onClick = {
                        navigator.user(user)
                    }
                )
                .width(size)
                .height(size)
        )
    }
}

fun Modifier.withAvatarClip(): Modifier {
    return composed {
        when (AmbientDisplayPreferences.current.avatarStyle) {
            DisplayPreferences.AvatarStyle.Round -> this.clip(CircleShape)
            DisplayPreferences.AvatarStyle.Square -> this.clip(MaterialTheme.shapes.medium)
            else -> this.clip(CircleShape)
        }
    }
}
