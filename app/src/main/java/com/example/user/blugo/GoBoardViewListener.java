package com.example.user.blugo;

import android.os.Handler;

/**
 * Created by user on 2016-06-07.
 */
public interface GoBoardViewListener {
    int MSG_VIEW_FULLY_DRAWN =  GoMessageListener.MSG_MAX + 1;
    int MSG_MAX = MSG_VIEW_FULLY_DRAWN;

    public Handler get_msg_handler();
}
