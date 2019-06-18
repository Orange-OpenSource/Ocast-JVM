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

package org.ocast.sample.desktop

import org.ocast.core.Device
import org.ocast.core.DeviceListener
import org.ocast.core.OCastCenter
import org.ocast.core.EventListener
import org.ocast.core.ReferenceDevice
import org.ocast.core.models.CustomEvent
import org.ocast.core.models.Media
import org.ocast.core.models.MetadataChangedEvent
import org.ocast.core.models.PlaybackStatusEvent
import org.ocast.core.models.UpdateStatusEvent
import org.ocast.core.utils.OCastLog
import java.util.concurrent.CountDownLatch
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

class AppKotlin : EventListener, DeviceListener {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val main = AppKotlin()
            main.run()
        }
    }

    private val latch = CountDownLatch(1)
    private val logger = Logger.getLogger("sampleAppKotlin")

    private val oCastCenter = OCastCenter()

    init {
        oCastCenter.addEventListener(this)
        oCastCenter.addDeviceListener(this)
        oCastCenter.registerDevice(ReferenceDevice::class.java)
        OCastLog.isEnabled = true
    }

    fun run() {
        try {
            logger.log(Level.INFO, "Application launched")
            oCastCenter.resumeDiscovery()
            latch.await()
        } catch (e: Exception) {
            oCastCenter.stopDiscovery()
            logger.log(Level.WARNING, "error:", e)
            Thread.currentThread().interrupt()
        }

        exitProcess(0)
    }

    private fun startApplication(device: Device) {
        device.applicationName = "Orange-DefaultReceiver-DEV"
        device.startApplication({
            prepareMedia(device)
        }, {
            oCastError -> logger.log(Level.WARNING, "startApplication error: ${oCastError.errorMessage}")
        })
    }

    private fun prepareMedia(device: Device) {
        device.prepareMedia("https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/mp4/BigBuckBunny.mp4",
            1,
            "Big Buck Bunny",
            "sampleAppKotlin",
            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            Media.Type.VIDEO,
            Media.TransferMode.STREAMED,
            true,
            null, {
                // ok
            }, {
                oCastError -> logger.log(Level.WARNING, "prepareMedia error", oCastError.status)
            })
    }

    override fun onPlaybackStatus(device: Device, status: PlaybackStatusEvent) {
        logger.log(Level.INFO, "[{${device.friendlyName}}] onPlaybackStatus: progress=${status.position} volume=${status.volume}")
        if (status.state === Media.PlayerState.IDLE) {
            latch.countDown()
        }
    }

    override fun onMetadataChanged(device: Device, metadata: MetadataChangedEvent) {
    }

    override fun onUpdateStatus(device: Device, updateStatus: UpdateStatusEvent) {
    }

    override fun onCustomEvent(device: Device, customEvent: CustomEvent) {
    }

    override fun onDeviceDisconnected(device: Device, error: Throwable?) {
    }

    override fun onDeviceAdded(device: Device) {
        startApplication(device)
    }

    override fun onDeviceRemoved(device: Device) {
    }
}
