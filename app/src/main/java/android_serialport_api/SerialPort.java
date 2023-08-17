/*
 * Copyright 2009 Cedric Priscal
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

package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {

    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudRate,  int flags) throws SecurityException, IOException {
        mFd = open(device.getAbsolutePath(), baudRate, flags);
        if (mFd == null) {
            Log.e("serial_port", device.getAbsolutePath() + " 串口打开异常");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    public boolean isOpen() {
        return mFd != null;
    }

    public void close2() {
        close();
        mFd = null;
        mFileInputStream = null;
        mFileOutputStream = null;
    }

    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    /**
     * 打开串口
     *
     * @param path     串口设备文件
     * @param baudRate 波特率
     * @param parity   奇偶校验，0 None（默认）； 1 Odd； 2 Even
     * @param dataBits 数据位，5 ~ 8  （默认8）
     * @param stopBit  停止位，1 或 2  （默认 1）
     * @param flags    标记 0（默认）
     */
//    private native static FileDescriptor open(String path, int baudRate, int parity, int dataBits, int stopBit, int flags);

    private native static FileDescriptor open(String path, int baudRate, int flags);
    private native void close();

    static {
        System.loadLibrary("serial_port");
    }
}
