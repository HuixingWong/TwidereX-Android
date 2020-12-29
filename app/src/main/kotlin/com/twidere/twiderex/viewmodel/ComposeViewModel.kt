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
package com.twidere.twiderex.viewmodel

import android.Manifest.permission
import android.location.*
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RequiresPermission
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.work.WorkManager
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.twidere.services.microblog.LookupService
import com.twidere.services.microblog.RelationshipService
import com.twidere.services.twitter.TwitterService
import com.twidere.twiderex.action.ComposeAction
import com.twidere.twiderex.db.model.DbDraft
import com.twidere.twiderex.di.assisted.IAssistedFactory
import com.twidere.twiderex.extensions.combineWith
import com.twidere.twiderex.extensions.getCachedLocation
import com.twidere.twiderex.model.AccountDetails
import com.twidere.twiderex.model.ComposeData
import com.twidere.twiderex.model.MicroBlogKey
import com.twidere.twiderex.model.ui.UiUser
import com.twidere.twiderex.repository.DraftRepository
import com.twidere.twiderex.repository.UserRepository
import com.twidere.twiderex.repository.twitter.TwitterTweetsRepository
import com.twidere.twiderex.scenes.ComposeType
import com.twidere.twiderex.worker.draft.SaveDraftWorker
import com.twitter.twittertext.Extractor
import java.util.UUID

class DraftItemViewModel @AssistedInject constructor(
    private val repository: DraftRepository,
    @Assisted private val draftId: String,
) : ViewModel() {

    val draft = liveData {
        repository.get(draftId)?.let {
            emit(it)
        }
    }

    @AssistedInject.Factory
    interface AssistedFactory : IAssistedFactory {
        fun create(
            draftId: String,
        ): DraftItemViewModel
    }
}

class DraftComposeViewModel @AssistedInject constructor(
    draftRepository: DraftRepository,
    locationManager: LocationManager,
    geocoder: Geocoder,
    composeAction: ComposeAction,
    factory: TwitterTweetsRepository.AssistedFactory,
    userRepositoryFactory: UserRepository.AssistedFactory,
    workManager: WorkManager,
    @Assisted account: AccountDetails,
    @Assisted private val draft: DbDraft,
) : ComposeViewModel(
    draftRepository,
    locationManager,
    geocoder,
    composeAction,
    factory,
    userRepositoryFactory,
    workManager,
    account,
    draft.statusKey,
    draft.composeType,
) {

    override val draftId: String = draft._id

    init {
        setText(draft.content)
        putImages(draft.media.map { Uri.parse(it) })
        excludedReplyUserIds.postValue(draft.excludedReplyUserIds ?: emptyList())
    }

    @AssistedInject.Factory
    interface AssistedFactory : IAssistedFactory {
        fun create(
            account: AccountDetails,
            draft: DbDraft,
        ): DraftComposeViewModel
    }
}

open class ComposeViewModel @AssistedInject constructor(
    protected val draftRepository: DraftRepository,
    private val locationManager: LocationManager,
    private val geocoder: Geocoder,
    protected val composeAction: ComposeAction,
    protected val factory: TwitterTweetsRepository.AssistedFactory,
    private val userRepositoryFactory: UserRepository.AssistedFactory,
    private val workManager: WorkManager,
    @Assisted protected val account: AccountDetails,
    @Assisted protected val statusKey: MicroBlogKey?,
    @Assisted val composeType: ComposeType,
) : ViewModel(), LocationListener {
    open val draftId: String = UUID.randomUUID().toString()

    @AssistedInject.Factory
    interface AssistedFactory : IAssistedFactory {
        fun create(
            account: AccountDetails,
            statusKey: MicroBlogKey?,
            composeType: ComposeType
        ): ComposeViewModel
    }

    protected val service by lazy {
        account.service as TwitterService
    }
    protected val repository by lazy {
        factory.create(
            account.accountKey,
            account.service as LookupService,
        )
    }

    protected val userRepository by lazy {
        userRepositoryFactory.create(
            accountKey = account.accountKey,
            account.service as LookupService,
            account.service as RelationshipService,
        )
    }

    val location = MutableLiveData<Address?>()
    val excludedReplyUserIds = MutableLiveData<List<String>>(emptyList())

    val replyToUserName = liveData {
        if (composeType == ComposeType.Reply && statusKey != null) {
            emitSource(
                status.map {
                    it?.let { status ->
                        Extractor().extractMentionedScreennames(
                            status.htmlText
                        ).filter { it != account.user.screenName }
                    } ?: run {
                        emptyList<String>()
                    }
                },
            )
        } else {
            emit(emptyList<String>())
        }
    }

    val replyToUser = liveData {
        emitSource(
            replyToUserName.switchMap {
                liveData {
                    if (it.isNotEmpty()) {
                        emit(userRepository.lookupUsersByName(it))
                    }
                }
            },
        )
    }
    val text = MutableLiveData("")
    val images = MutableLiveData<List<Uri>>(emptyList())
    val canSaveDraft = text.combineWith(images) { text, imgs -> !text.isNullOrEmpty() || !imgs.isNullOrEmpty() }
    val locationEnabled = MutableLiveData(false)
    val status = liveData {
        statusKey?.let {
            emitSource(repository.loadTweetFromCache(it))
        } ?: run {
            emit(null)
        }
    }

    fun setText(value: String) {
        text.postValue(value)
    }

    fun compose() {
        text.value?.let {
            composeAction.commit(
                account.accountKey,
                ComposeData(
                    content = it,
                    draftId = draftId,
                    images = images.value?.map { it.toString() } ?: emptyList(),
                    composeType = composeType,
                    statusKey = statusKey,
                    lat = location.value?.latitude,
                    long = location.value?.longitude,
                    excludedReplyUserIds = excludedReplyUserIds.value
                )
            )
        }
    }

    fun saveDraft() {
        text.value?.let { text ->
            workManager
                .beginWith(
                    SaveDraftWorker.create(
                        ComposeData(
                            content = text,
                            draftId = draftId,
                            images = images.value?.map { it.toString() } ?: emptyList(),
                            composeType = composeType,
                            statusKey = statusKey,
                            lat = location.value?.latitude,
                            long = location.value?.longitude,
                            excludedReplyUserIds = excludedReplyUserIds.value
                        )
                    )
                )
                .enqueue()
        }
    }

    fun putImages(value: List<Uri>) {
        images.value?.let {
            value + it
        }?.let {
            it.take(4)
        }?.let {
            images.postValue(it)
        }
    }

    @RequiresPermission(anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    fun trackingLocation() {
        locationEnabled.postValue(true)
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        val provider = locationManager.getBestProvider(criteria, true) ?: return
        locationManager.requestLocationUpdates(provider, 0, 0f, this)
        locationManager.getCachedLocation()?.let {
            decodeLocation(it)
        }
    }

    private fun decodeLocation(it: Location) {
        geocoder.getFromLocation(it.latitude, it.longitude, 1)
            .getOrNull(0)?.let {
                location.postValue(it)
            }
    }

    fun disableLocation() {
        location.postValue(null)
        locationEnabled.postValue(false)
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        decodeLocation(location)
    }

    // compatibility fix for Api < 22
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onCleared() {
        if (locationEnabled.value == true) {
            locationManager.removeUpdates(this)
        }
    }

    fun removeImage(item: Uri) {
        images.value?.let {
            it - item
        }?.let {
            images.postValue(it)
        }
    }

    fun excludeReplyUser(user: UiUser) {
        excludedReplyUserIds.value?.let {
            excludedReplyUserIds.postValue(it + user.id)
        }
    }

    fun includeReplyUser(user: UiUser) {
        excludedReplyUserIds.value?.let {
            excludedReplyUserIds.postValue(it - user.id)
        }
    }
}
