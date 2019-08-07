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

package org.ocast.sdk.core.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.util.EnumSet
import org.ocast.sdk.core.utils.BitflagsSerializer

//region Messages

/**
 * Represents a public settings input message.
 *
 * @param T The type of params.
 * @param data The data layer conveyed by the public settings input message.
 * @constructor Creates an instance of [InputMessage].
 */
class InputMessage<T>(data: OCastDataLayer<T>) : OCastApplicationLayer<T>(SettingsService.INPUT, data)

/**
 * Represents a public settings device message.
 *
 * @param T The type of params.
 * @param data The data layer conveyed by the public settings device message.
 * @constructor Creates an instance of [DeviceMessage].
 */
class DeviceMessage<T>(data: OCastDataLayer<T>) : OCastApplicationLayer<T>(SettingsService.DEVICE, data)

//endregion

//region Commands

/**
 * Represents the parameters of a `getUpdateStatus` command.
 *
 * @constructor Creates an instance of [GetUpdateStatusCommandParams].
 */
class GetUpdateStatusCommandParams : OCastCommandParams("getUpdateStatus")

/**
 * Represents the parameters of a `getDeviceID` command.
 *
 * @constructor Creates an instance of [GetDeviceIDCommandParams].
 */
class GetDeviceIDCommandParams : OCastCommandParams("getDeviceID")

/**
 * Represents the parameters of a `keyPressed` command.
 *
 * @property key The key attribute value corresponding to the key pressed. Possible supported keys are defined by the [W3C UI events KeyboardEvent interface](https://www.w3.org/TR/uievents-code/).
 * @property code A string that identifies the physical key being pressed.
 * @property ctrl Indicates if the `Control` key is pressed.
 * @property alt Indicates if the `Alt` key is pressed.
 * @property shift Indicates if the `Shift` key is pressed.
 * @property meta Indicates if the `Meta` key (OS specific) is pressed.
 * @property location The location of the key on the keyboard. This is used to disambiguate between key values that can be generated by different physical keys on the keyboard, for example, the left and right `Shift` key.
 * @constructor Creates an instance of [SendKeyEventCommandParams].
 */
class SendKeyEventCommandParams(
    @JsonProperty("key") val key: String,
    @JsonProperty("code") val code: String,
    @JsonProperty("ctrl") val ctrl: Boolean,
    @JsonProperty("alt") val alt: Boolean,
    @JsonProperty("shift") val shift: Boolean,
    @JsonProperty("meta") val meta: Boolean,
    @JsonProperty("location") val location: DOMKeyLocation
) : OCastCommandParams("keyPressed") {

    /**
     * Represents the location of a key on the keyboard.
     *
     * @property value The raw key location value.
     */
    enum class DOMKeyLocation(private val value: Int) {

        /**
         * The key activation MUST NOT be distinguished as the left or right version of the key,
         * and (other than the `NumLock` key) did not originate from the numeric keypad.
         */
        STANDARD(0),

        /**
         * The key activated originated from the left key location
         * (when there is more than one possible location for this key).
         */
        LEFT(1),

        /**
         * The key activation originated from the right key location
         * (when there is more than one possible location for this key).
         */
        RIGHT(2),

        /**
         * The key activation originated on the numeric keypad or with a virtual key corresponding to the numeric keypad
         * (when there is more than one possible location for this key).
         * Note that the NumLock key should always be encoded with a location of `STANDARD`.
         */
        NUMPAD(3);

        /**
         * Returns the raw key location value.
         */
        @JsonValue
        fun toValue() = value
    }
}

/**
 * Represents the parameters of a `mouseEvent` command.
 *
 * @property x The horizontal coordinate at which the event occurred relative to the origin of the screen coordinate system.
 * @property y The vertical coordinate at which the event occurred relative to the origin of the screen coordinate system.
 * @property buttons The combination of mouse buttons clicked.
 * @constructor Creates an instance of [SendMouseEventCommandParams].
 */
class SendMouseEventCommandParams(
    @JsonProperty("x") val x: Int,
    @JsonProperty("y") val y: Int,
    @JsonSerialize(using = BitflagsSerializer::class)
    @JsonProperty("buttons") val buttons: EnumSet<Button>
) : OCastCommandParams("mouseEvent") {

    /**
     * Represents a mouse button.
     */
    enum class Button(override val bit: Int) : Bitflag {

        /** The primary / left button. */
        PRIMARY(0),

        /** The secondary / right button. */
        RIGHT(1),

        /** The auxiliary / middle button. */
        MIDDLE(2);

        /**
         * Returns the raw button value.
         */
        @JsonValue
        fun toValue() = bit
    }
}

/**
 * Represents the parameters of a `gamepadEvent` command.
 *
 * @property axes The axes of the gamepad.
 * @property buttons The combination of gamepad buttons pressed.
 * @constructor Creates an instance of [SendGamepadEventCommandParams].
 */
class SendGamepadEventCommandParams(
    @JsonProperty("axes") val axes: List<Axe>,
    @JsonSerialize(using = BitflagsSerializer::class)
    @JsonProperty("buttons") val buttons: EnumSet<Button>
) : OCastCommandParams("gamepadEvent") {

    /**
     * Represents a gamepad axis.
     *
     * @property x The horizontal axis value, ranging from -1.0 (left) to 1.0 (right).
     * @property y The vertical axis value, ranging from -1.0 (up) to 1.0 (down).
     * @property type The axis type.
     */
    class Axe(
        @JsonProperty("x") val x: Double,
        @JsonProperty("y") val y: Double,
        @JsonProperty("num") val type: Type
    ) {

        /**
         * Represents a type of axis.
         *
         * @property value The raw axis type value.
         */
        enum class Type(private val value: Int) {

            /** The horizontal axis of the left stick. */
            LEFT_STICK_HORIZONTAL(0),

            /** The vertical axis of the left stick. */
            LEFT_STICK_VERTICAL(1),

            /** The horizontal axis of the right stick. */
            RIGHT_STICK_HORIZONTAL(2),

            /** The vertical axis of the right stick. */
            RIGHT_STICK_VERTICAL(3);

            /**
             * Returns the raw axis type value.
             */
            @JsonValue
            fun toValue() = value
        }
    }

    /**
     * Represents a gamepad button.
     */
    enum class Button(override val bit: Int) : Bitflag {

        /** The bottom button in the right cluster. */
        RIGHT_CLUSTER_BOTTOM(0),

        /** The right button in the right cluster. */
        RIGHT_CLUSTER_RIGHT(1),

        /** The left button in the right cluster. */
        RIGHT_CLUSTER_LEFT(2),

        /** The top button in the right cluster. */
        RIGHT_CLUSTER_TOP(3),

        /** The top left front button. */
        TOP_LEFT_FRONT(4),

        /** The top right front button. */
        TOP_RIGHT_FRONT(5),

        /** The bottom left front button. */
        BOTTOM_LEFT_FRONT(6),

        /** The bottom right front button. */
        BOTTOM_RIGHT_FRONT(7),

        /** The left button in the center cluster. */
        CENTER_CLUSTER_LEFT(8),

        /** The right button in the center cluster. */
        CENTER_CLUSTER_RIGHT(9),

        /** The left stick pressed button. */
        LEFT_STICK_PRESSED(10),

        /** The right stick pressed button. */
        RIGHT_STICK_PRESSED(11),

        /** The top button in the left cluster. */
        LEFT_CLUSTER_TOP(12),

        /** The bottom button in the left cluster. */
        LEFT_CLUSTER_BOTTOM(13),

        /** The left button in the left cluster. */
        LEFT_CLUSTER_LEFT(14),

        /** The right button in the left cluster. */
        LEFT_CLUSTER_RIGHT(15),

        /** The middle button in the center cluster. */
        CENTER_CLUSTER_MIDDLE(16);

        /**
         * Returns the raw button value.
         */
        @JsonValue
        fun toValue() = bit
    }
}

//endregion

//region Replies and events

/**
 * Represents the firmware update status of a device.
 *
 * @property state The firmware update state.
 * @property version The version of the firmware to update.
 * @property progress The download progress. Only available if state equals `DOWNLOADING`.
 * @constructor Creates an instance of [UpdateStatus].
 */
class UpdateStatus(
    @JsonProperty("state") val state: State,
    @JsonProperty("version") val version: String,
    @JsonProperty("progress") val progress: Int
) {

    /**
     * Represents the state of a firmware update.
     *
     * @property value The raw state value.
     */
    enum class State(private val value: String) {

        /** The firmware was successfully updated. */
        SUCCESS("success"),

        /** There was an error while updating the firmware. */
        ERROR("error"),

        /** The firmware update status has not been checked yet. */
        NOT_CHECKED("notChecked"),

        /** The firmware is up to date. */
        UP_TO_DATE("upToDate"),

        /** A firmware update is available. */
        NEW_VERSION_FOUND("newVersionFound"),

        /** The firmware update is being downloaded. */
        DOWNLOADING("downloading"),

        /** The firmware update is downloaded and ready to be installed. */
        NEW_VERSION_READY("newVersionReady");

        /**
         * Returns the raw firmware update state value.
         */
        @JsonValue
        fun toValue() = value
    }
}

/**
 * Represents the device identifier.
 *
 * @property id The device identifier.
 * @constructor Creates an instance of [DeviceID].
 */
class DeviceID(
    @JsonProperty("id") val id: String
)

//endregion
