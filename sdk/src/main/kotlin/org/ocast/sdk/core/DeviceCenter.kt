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

package org.ocast.sdk.core

import java.util.Collections
import org.json.JSONObject
import org.ocast.sdk.common.extensions.ifNotNull
import org.ocast.sdk.core.models.MediaMetadata
import org.ocast.sdk.core.models.MediaPlaybackStatus
import org.ocast.sdk.core.models.UpdateStatus
import org.ocast.sdk.core.wrapper.CallbackWrapper
import org.ocast.sdk.core.wrapper.CallbackWrapperOwner
import org.ocast.sdk.core.wrapper.SimpleCallbackWrapper
import org.ocast.sdk.discovery.DeviceDiscovery
import org.ocast.sdk.discovery.models.UpnpDevice

open class DeviceCenter : CallbackWrapperOwner {

    companion object {

        /** The default value for the discovery interval. */
        const val DEFAULT_DISCOVERY_INTERVAL = DeviceDiscovery.DEFAULT_INTERVAL

        /** The minimum value for the discovery interval. */
        const val MINIMUM_DISCOVERY_INTERVAL = DeviceDiscovery.MINIMUM_INTERVAL
    }

    override var callbackWrapper: CallbackWrapper = SimpleCallbackWrapper()
        set(value) {
            field = value
            devices.forEach { it.callbackWrapper = value }
        }

    private val deviceDiscovery = DeviceDiscovery()

    private val eventListeners = mutableSetOf<EventListener>()
    private val deviceListeners = mutableSetOf<DeviceListener>()
    private var oCastCenterListeners = mutableSetOf<OCastCenterListener>()

    private val registeredDevicesByManufacturer = mutableMapOf<String, Class<out Device>>()
    private val detectedDevices = Collections.synchronizedList(mutableListOf<Device>())

    val devices: List<Device>
        get() = detectedDevices.toList()

    var discoveryInterval: Long
        get() = deviceDiscovery.interval
        set(value) {
            deviceDiscovery.interval = value
        }

    private fun createDevice(device: UpnpDevice): Device? {
        return registeredDevicesByManufacturer[device.manufacturer]
            ?.getConstructor(UpnpDevice::class.java)
            ?.newInstance(device)
            ?.apply {
                deviceListener = this@DeviceCenter.deviceListener
                eventListener = this@DeviceCenter.eventListener
                callbackWrapper = this@DeviceCenter.callbackWrapper
                // Custom actions on custom device
                onCreateDevice(this)
                detectedDevices.add(this)
            }
    }

    private fun removeDevice(device: Device) {
        device.apply {
            deviceListener = null
            eventListener = null
            // Custom actions on custom device
            onRemoveDevice(this)
            detectedDevices.remove(this)
        }
    }

    protected open fun onCreateDevice(device: Device) {
    }

    protected open fun onRemoveDevice(device: Device) {
    }

    fun registerDevice(deviceClass: Class<out Device>) {
        val device = deviceClass.getConstructor(UpnpDevice::class.java).newInstance(UpnpDevice())
        registeredDevicesByManufacturer[device.manufacturer] = deviceClass
        deviceDiscovery.searchTargets += device.searchTarget
    }

    fun addEventListener(listener: EventListener) {
        eventListeners.add(listener)
    }

    fun removeEventListener(listener: EventListener) {
        eventListeners.remove(listener)
    }

    fun addDeviceListener(listener: DeviceListener) {
        deviceListeners.add(listener)
    }

    fun removeDeviceListener(listener: DeviceListener) {
        deviceListeners.remove(listener)
    }

    fun addOCastCenterListener(oCastCenterListener: OCastCenterListener) {
        oCastCenterListeners.add(oCastCenterListener)
    }

    fun removeOCastCenterListener(oCastCenterListener: OCastCenterListener) {
        oCastCenterListeners.remove(oCastCenterListener)
    }

    fun resumeDiscovery(): Boolean {
        deviceDiscovery.listener = deviceDiscoveryListener

        return deviceDiscovery.resume()
    }

    fun stopDiscovery(): Boolean {
        deviceDiscovery.listener = null

        return deviceDiscovery.stop()
    }

    fun pauseDiscovery(): Boolean {
        return deviceDiscovery.pause()
    }

    //region Discovery listener

    private val deviceDiscoveryListener = object : DeviceDiscovery.Listener {

        override fun onDevicesAdded(devices: List<UpnpDevice>) {
            devices.forEach { upnpDevice ->
                createDevice(upnpDevice)?.ifNotNull { device ->
                    this@DeviceCenter.deviceListener.onDeviceAdded(device)
                }
            }
        }

        override fun onDevicesRemoved(devices: List<UpnpDevice>) {
            devices.forEach { device ->
                synchronized(detectedDevices) {
                    detectedDevices.firstOrNull { device.id == it.upnpID }?.ifNotNull {
                        this@DeviceCenter.deviceListener.onDeviceRemoved(it)
                        removeDevice(it)
                    }
                }
            }
        }

        override fun onDiscoveryStopped(error: Throwable?) {
            oCastCenterListeners.wrapForEach { it.onDiscoveryStopped(error) }
        }
    }

    //endregion

    //region Event listener

    private val eventListener = object : EventListener {

        override fun onMediaPlaybackStatus(device: Device, mediaPlaybackStatus: MediaPlaybackStatus) {
            eventListeners.wrapForEach { it.onMediaPlaybackStatus(device, mediaPlaybackStatus) }
        }

        override fun onMediaMetadataChanged(device: Device, mediaMetadata: MediaMetadata) {
            eventListeners.wrapForEach { it.onMediaMetadataChanged(device, mediaMetadata) }
        }

        override fun onUpdateStatus(device: Device, updateStatus: UpdateStatus) {
            eventListeners.wrapForEach { it.onUpdateStatus(device, updateStatus) }
        }

        override fun onCustomEvent(device: Device, name: String, params: JSONObject) {
            eventListeners.wrapForEach { it.onCustomEvent(device, name, params) }
        }
    }

    //endregion

    //region Device listener

    private val deviceListener = object : DeviceListener {

        override fun onDeviceAdded(device: Device) {
            deviceListeners.wrapForEach { it.onDeviceAdded(device) }
        }

        override fun onDeviceRemoved(device: Device) {
            deviceListeners.wrapForEach { it.onDeviceRemoved(device) }
        }

        override fun onDeviceDisconnected(device: Device, error: Throwable?) {
            deviceListeners.wrapForEach { it.onDeviceDisconnected(device, error) }
        }
    }

    //endregion
}

interface OCastCenterListener {

    fun onDiscoveryStopped(error: Throwable?)
}
