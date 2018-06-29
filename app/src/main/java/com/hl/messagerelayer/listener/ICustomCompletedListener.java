package com.hl.messagerelayer.listener;

/**
 * Created by heliu on 2018/3/30.
 */

public interface ICustomCompletedListener {
    void success();

    void failed(String msg);
}
