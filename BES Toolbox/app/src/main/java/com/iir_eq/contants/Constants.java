package com.iir_eq.contants;

import java.util.UUID;

/**
 * Created by zhaowanxing on 2017/7/18.
 */

public class Constants {
    public static final UUID BES_SERVICE_UUID = UUID.fromString("01000100-0000-1000-8000-009078563412");
    public static final UUID BES_CHARACTERISTIC_TX_UUID = UUID.fromString("03000300-0000-1000-8000-009278563412");
    public static final UUID BES_CHARACTERISTIC_RX_UUID = UUID.fromString("02000200-0000-1000-8000-009178563412");
    public static final UUID BES_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID OTA_SERVICE_NORMAL_UUID = UUID.fromString("01000100-0000-1000-8000-009078563412");
    public static final UUID OTA_CHARACTERISTIC_NORMAL_UUID = UUID.fromString("03000300-0000-1000-8000-009278563412");
    public static final UUID OTA_SERVICE_OTA_UUID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    public static final UUID OTA_CHARACTERISTIC_OTA_UUID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    public static final UUID OTA_DESCRIPTOR_OTA_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final boolean DEFAULT_CLEAR_USER_DATA = true;
    public static final boolean DEFAULT_UPDATE_BT_ADDRESS = false;
    public static final boolean DEFAULT_UPDATE_BT_NAME = false;
    public static final boolean DEFAULT_UPDATE_BLE_ADDRESS = false;
    public static final boolean DEFAULT_UPDATE_BLE_NAME = false;
    public static final byte[] DEFAULT_OTA_RESUME_VERTIFY_RANDOM_CODE = null;

    public static final String DEFAULT_OTA_DAUL_WAY = "";

    public static final String KEY_OTA_CONFIG_CLEAR_USER_DATA = "ota_config_clear_user_data";
    public static final String KEY_OTA_CONFIG_UPDATE_BT_ADDRESS = "ota_config_update_bt_address";
    public static final String KEY_OTA_CONFIG_UPDATE_BT_ADDRESS_VALUE = "ota_config_update_bt_address_value";
    public static final String KEY_OTA_CONFIG_UPDATE_BT_NAME = "ota_config_update_bt_name";
    public static final String KEY_OTA_CONFIG_UPDATE_BT_NAME_VALUE = "ota_config_update_bt_name_value";
    public static final String KEY_OTA_CONFIG_UPDATE_BLE_ADDRESS = "ota_config_update_ble_address";
    public static final String KEY_OTA_CONFIG_UPDATE_BLE_ADDRESS_VALUE = "ota_config_update_ble_address_value";
    public static final String KEY_OTA_CONFIG_UPDATE_BLE_NAME = "ota_config_update_ble_name";
    public static final String KEY_OTA_CONFIG_UPDATE_BLE_NAME_VALUE = "ota_config_update_ble_name_value";
    public static final String KEY_OTA_RESUME_VERTIFY_RANDOM_CODE = "ota_resume_vertify_random_code";

    public static final String KEY_OTA_DAUL_APPLY_WAY = "ota_daul_apply_way";

    public static final String KEY_OTA_DAUL_LEFT_FILE = "ota_file_daul_left";
    public static final String KEY_OTA_DAUL_RIGHT_FILE = "ota_file_daul_right";

}
