package com.melo.activemq;

import javax.jms.Message;
import java.util.concurrent.Semaphore;

/**
 * 应答报文
 * Created by Ablert
 * on 2018/4/20.
 */
public class ReplyMessage {

    private Semaphore semaphore = new Semaphore(0);

    private Message message;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }
}
