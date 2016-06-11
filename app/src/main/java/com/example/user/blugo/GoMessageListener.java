package com.example.user.blugo;

import android.os.Handler;

/**
 * Created by user on 2016-06-08.
 */
public interface GoMessageListener {
    int MSG_VIEW_FULLY_DRAWN =  1;
    int BLUTOOTH_SERVER_SOCKET_ERROR = 2;
    int BLUTOOTH_CLIENT_SOCKET_ERROR = 3;
    int BLUTOOTH_CLIENT_CONNECT_SUCCESS = 4;
    int BLUTOOTH_COMM_ERROR = 5;
    int BLUTOOTH_COMM_MSG = 6;
    int BLUTOOTH_COMM_ACCEPTED_REQUEST = 7;
    int FRONTDOORACTIVITY_MSG_LOAD_END = 8;
    int SAVE_FILE_NAME_INPUT_FINISHED = 9;
    int MSG_MAX = SAVE_FILE_NAME_INPUT_FINISHED;


    public final static String GAME_SETTING_MESSAGE =
        "com.example.user.blugo.GoMessageListener.GAME_SETTING_MESSAGE";

    Handler get_msg_handler();
}
