package com.ww.mall.kafka.exception;

/**
 * 自定义Kafka异常类
 */
public class KafkaException extends RuntimeException {
    
    /**
     * 默认构造方法
     */
    public KafkaException() {
        super();
    }
    
    /**
     * 带消息的构造方法
     *
     * @param message 错误消息
     */
    public KafkaException(String message) {
        super(message);
    }
    
    /**
     * 带消息和原因的构造方法
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public KafkaException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 带原因的构造方法
     *
     * @param cause 原因
     */
    public KafkaException(Throwable cause) {
        super(cause);
    }
} 