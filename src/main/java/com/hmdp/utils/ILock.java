package com.hmdp.utils;

/**
 * @author zhusiyuan
 * @date 2023/2/23
 * @apiNote
 */
public interface ILock {
    boolean tryLock(long timeoutSec);

    void unlock();
}
