/*
 * Copyright 2019 Orange
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

package org.ocast.sample.mobile.ui

import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatDelegate
import android.support.v7.app.MediaRouteActionProvider
import android.support.v7.media.MediaRouter
import android.util.Log
import android.view.Menu
import org.ocast.core.Device
import org.ocast.core.EventListener
import org.ocast.core.ReferenceDevice
import org.ocast.core.models.CustomEvent
import org.ocast.core.models.Media
import org.ocast.core.models.MetadataChangedEvent
import org.ocast.core.models.PlaybackStatusEvent
import org.ocast.core.models.UpdateStatusEvent
import org.ocast.mediaroute.OCastMediaRouteHelper
import org.ocast.sample.mobile.R
import org.ocast.sample.mobile.databinding.ActivityMainBinding
import org.ocast.sample.mobile.utils.updateValue

class MainActivity : AppCompatActivity(), EventListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel: MainViewModel

    private var mediaRouterCallback: MediaRouter.Callback = MediaRouterCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            this.lifecycleOwner = this@MainActivity
            this.viewModel = mainViewModel
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        OCastMediaRouteHelper.init(this, listOf(ReferenceDevice::class.java))
        OCastMediaRouteHelper.addMediaRouterCallback(mediaRouterCallback)
    }

    override fun onStart() {
        super.onStart()

        OCastMediaRouteHelper.addEventListener(this)
    }

    override fun onStop() {
        super.onStop()

        OCastMediaRouteHelper.removeEventListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        OCastMediaRouteHelper.removeMediaRouterCallback(mediaRouterCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        val mediaRouteMenuItem = menu.findItem(R.id.item_all_media_route)
        val actionProvider = MenuItemCompat.getActionProvider(mediaRouteMenuItem) as MediaRouteActionProvider

        // Set the MediaRouteActionProvider selector for device discovery.
        actionProvider.routeSelector = OCastMediaRouteHelper.mediaRouteSelector

        return true
    }

    private fun startApplication(device: Device) {
        device.applicationName = "Orange-DefaultReceiver-DEV"
        device.startApplication({
            prepareMedia(device)
        }, {
            oCastError -> Log.e(TAG, "startApplication error $oCastError.errorMessage")
        })
    }

    private fun prepareMedia(device: Device) {
        device.connect({
            device.prepareMedia("https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/mp4/BigBuckBunny.mp4",
                1,
                "Big Buck Bunny",
                "sampleAppKotlin",
                "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                Media.Type.VIDEO,
                Media.TransferMode.STREAMED,
                true,
                null, {
                    mainViewModel.deviceConnected.updateValue(true)
                }, {
                    oCastError -> Log.e(TAG, "prepareMedia error $oCastError.status")
                })
        }, {
            oCastError -> Log.e(TAG, "connect error ${oCastError.errorMessage}")
        })
    }

    override fun onPlaybackStatus(device: Device, status: PlaybackStatusEvent) {
        if (mainViewModel.selectedDevice.value == device) {
            mainViewModel.playbackStatus.updateValue(status)
        }
    }

    override fun onMetadataChanged(device: Device, metadata: MetadataChangedEvent) {
        if (mainViewModel.selectedDevice.value == device) {
            mainViewModel.mediaMetadata.updateValue(metadata)
        }
    }

    override fun onUpdateStatus(device: Device, updateStatus: UpdateStatusEvent) {
    }

    override fun onCustomEvent(device: Device, customEvent: CustomEvent) {
    }

    private inner class MediaRouterCallback : MediaRouter.Callback() {

        override fun onRouteSelected(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
            OCastMediaRouteHelper.getDeviceFromRoute(route)?.apply {
                Log.d(TAG, "OCast device selected: $friendlyName")
                mainViewModel.selectedDevice.updateValue(this)
                startApplication(this)
            }
        }

        override fun onRouteUnselected(mediaRouter: MediaRouter?, route: MediaRouter.RouteInfo?) {
            OCastMediaRouteHelper.getDeviceFromRoute(route)?.apply {
                Log.d(TAG, "OCast device unselected")
                stopApplication({}, {})
                mainViewModel.deviceConnected.updateValue(false)
            }
        }

        override fun onRouteRemoved(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
            if (OCastMediaRouteHelper.isOCastRouteInfo(route)) {
                Log.d(TAG, "OCast device removed")
            }
        }

        override fun onRouteAdded(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
            if (OCastMediaRouteHelper.isOCastRouteInfo(route)) {
                Log.d(TAG, "OCast device added")
            }
        }

        override fun onRouteChanged(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
            if (OCastMediaRouteHelper.isOCastRouteInfo(route)) {
                Log.d(TAG, "OCast device changed")
            }
        }
    }
}
