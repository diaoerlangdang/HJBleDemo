package com.wise.ble.scan;




import androidx.annotation.IntRange;

import static com.wise.ble.scan.BackgroundPowerSaver.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
import static com.wise.ble.scan.BackgroundPowerSaver.DEFAULT_BACKGROUND_SCAN_PERIOD;
import static com.wise.ble.scan.BackgroundPowerSaver.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
import static com.wise.ble.scan.BackgroundPowerSaver.DEFAULT_FOREGROUND_SCAN_PERIOD;

/**
 * Copyright (C) BlakeQu All Rights Reserved <blakequ@gmail.com>
 * <p>
 * Licensed under the blakequ.com License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * author  : quhao <blakequ@gmail.com> <br>
 * date     : 2016/12/1 14:28 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description: config of ble connect and scan
 */

public class BleParamsOptions {

    private final boolean isDebugMode;

    //using for scan
    private final long foregroundScanPeriod;
    private final long foregroundBetweenScanPeriod;
    private final long backgroundScanPeriod;
    private final long backgroundBetweenScanPeriod;


    public boolean isDebugMode() {
        return isDebugMode;
    }

    public long getForegroundScanPeriod() {
        return foregroundScanPeriod;
    }

    public long getForegroundBetweenScanPeriod() {
        return foregroundBetweenScanPeriod;
    }

    public long getBackgroundScanPeriod() {
        return backgroundScanPeriod;
    }

    public long getBackgroundBetweenScanPeriod() {
        return backgroundBetweenScanPeriod;
    }


    private BleParamsOptions(Builder builder){
        this.isDebugMode = builder.isDebugMode;
        this.foregroundScanPeriod = builder.foregroundScanPeriod;
        this.foregroundBetweenScanPeriod = builder.foregroundBetweenScanPeriod;
        this.backgroundScanPeriod = builder.backgroundScanPeriod;
        this.backgroundBetweenScanPeriod = builder.backgroundBetweenScanPeriod;

    }

    public static class Builder {

        private boolean isDebugMode = true;
        private long foregroundScanPeriod = DEFAULT_FOREGROUND_SCAN_PERIOD;
        private long foregroundBetweenScanPeriod = DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
        private long backgroundScanPeriod = DEFAULT_BACKGROUND_SCAN_PERIOD;
        private long backgroundBetweenScanPeriod = DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;


        /**
         * setting is debug mode, if false then the log will disable
         * @param isDebugMode you can set by BuildConfig.DEBUG, default is true
         * @return
         */
        public Builder setDebugMode(boolean isDebugMode){
            this.isDebugMode = isDebugMode;
            return this;
        }

        /**
         * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
         * This function is used to setup the period when switching
         * between background/foreground. To have it effect on an already running scan (when the next
         * cycle starts)
         *
         * @param foregroundScanPeriod defalut is 10 seconds, you should using milliseconds
         * @return
         */
        public Builder setForegroundScanPeriod(@IntRange(from = 0) long foregroundScanPeriod) {
            if (foregroundScanPeriod < 0){
                throw new IllegalArgumentException("Period time must > 0, now is "+foregroundScanPeriod);
            }
            this.foregroundScanPeriod = foregroundScanPeriod;
            return this;
        }

        /**
         * Sets the duration in milliseconds between each Bluetooth LE scan cycle to look for beacons.
         * This function is used to setup the period when switching
         * between background/foreground. To have it effect on an already running scan (when the next
         * cycle starts)
         * @param foregroundBetweenScanPeriod defalut is 5 seconds, you should using milliseconds
         * @return
         */
        public Builder setForegroundBetweenScanPeriod(@IntRange(from = 0) long foregroundBetweenScanPeriod) {
            if (foregroundBetweenScanPeriod < 0){
                throw new IllegalArgumentException("Period time must > 0, now is "+foregroundBetweenScanPeriod);
            }
            this.foregroundBetweenScanPeriod = foregroundBetweenScanPeriod;
            return this;
        }

        /**
         * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
         * This function is used to setup the period when switching
         * between background/foreground. To have it effect on an already running scan (when the next
         * cycle starts)
         * @param backgroundScanPeriod default is 10 seconds, you should using milliseconds
         * @return
         */
        public Builder setBackgroundScanPeriod(@IntRange(from = 0) long backgroundScanPeriod) {
            if (backgroundScanPeriod < 0){
                throw new IllegalArgumentException("Period time must > 0, now is "+backgroundScanPeriod);
            }
            this.backgroundScanPeriod = backgroundScanPeriod;
            return this;
        }

        /**
         * Sets the duration in milliseconds spent not scanning between each Bluetooth LE scan cycle when no ranging/monitoring clients are in the foreground
         * @param backgroundBetweenScanPeriod default is 5 minutes, you should using milliseconds
         * @return
         */
        public Builder setBackgroundBetweenScanPeriod(@IntRange(from = 0) long backgroundBetweenScanPeriod) {
            if (backgroundBetweenScanPeriod < 0){
                throw new IllegalArgumentException("Period time must > 0, now is "+backgroundBetweenScanPeriod);
            }
            this.backgroundBetweenScanPeriod = backgroundBetweenScanPeriod;
            return this;
        }


        /** Builds configured {@link BleParamsOptions} object */
        public BleParamsOptions build() {
            return new BleParamsOptions(this);
        }
    }

    public static BleParamsOptions createDefault() {
        return new Builder().build();
    }

}
