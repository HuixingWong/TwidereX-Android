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
package com.twidere.twiderex.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.collectAsLazyPagingItems
import com.twidere.twiderex.component.lazy.itemsPaging
import com.twidere.twiderex.component.navigation.AmbientNavigator
import com.twidere.twiderex.component.status.UserAvatar
import com.twidere.twiderex.ui.mediumEmphasisContentContentColor
import com.twidere.twiderex.viewmodel.UserListViewModel

@Composable
fun UserListComponent(
    viewModel: UserListViewModel,
) {
    val source = viewModel.source.collectAsLazyPagingItems()
    val navigator = AmbientNavigator.current
    LazyColumn {
        itemsPaging(source) {
            it?.let {
                ListItem(
                    modifier = Modifier.clickable {
                        navigator.user(it)
                    },
                    icon = {
                        UserAvatar(
                            user = it,
                        )
                    },
                    text = {
                        Row {
                            Text(
                                text = it.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colors.primary,
                            )
                            Text(
                                text = "@${it.screenName}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = mediumEmphasisContentContentColor,
                            )
                        }
                    },
                    secondaryText = {
                        Text("followers: ${it.followersCount}")
                    }
                )
            }
        }
    }
}
