package com.melo.activemq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 生产者
 * Created by Ablert
 * on 2018/4/20.
 */
public class Producer implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Producer.class);

    private JmsTemplate jmsTemplate;

    private Destination requestDestination;

    private Destination replyDestination;

    private static ConcurrentHashMap<String, ReplyMessage> concurrentHashMap = new ConcurrentHashMap<String, ReplyMessage>();

    public JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public Destination getRequestDestination() {
        return requestDestination;
    }

    public void setRequestDestination(Destination requestDestination) {
        this.requestDestination = requestDestination;
    }

    public Destination getReplyDestination() {
        return replyDestination;
    }

    public void setReplyDestination(Destination replyDestination) {
        this.replyDestination = replyDestination;
    }

    /**
     * 发送消息
     * 给内部类传入参数需要将参数设置为final类型
     * @param message
     * @return
     */
    public String sendMessage(final String message) {
        ReplyMessage replyMessage = new ReplyMessage();
        final String correlationId = UUID.randomUUID().toString();
        concurrentHashMap.put(correlationId, replyMessage);
        jmsTemplate.send(requestDestination, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                Message msg = session.createTextMessage(message);
                msg.setJMSCorrelationID(correlationId);
                msg.setJMSReplyTo(replyDestination);
                return msg;
            }
        });
        try {
            boolean isReceiveMessage = replyMessage.getSemaphore().tryAcquire(10, TimeUnit.SECONDS);
            ReplyMessage result = concurrentHashMap.get(correlationId);
            if (isReceiveMessage && null != result) {
                Message msg = result.getMessage();
                if (null != msg) {
                    TextMessage textMessage = (TextMessage) msg;
                    return textMessage.getText();
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("interrupt failure", e);
            e.printStackTrace();
        } catch (JMSException e) {
            LOGGER.error("get msg failure", e);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            try {
                concurrentHashMap.get(textMessage.getJMSCorrelationID()).setMessage(message );
                concurrentHashMap.get(textMessage.getJMSCorrelationID()).getSemaphore().release();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }
}
