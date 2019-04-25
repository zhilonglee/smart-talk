package com.zhilong.smarttalk.netty;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ChatMsg implements Serializable {

    private static final long serialVersionUID = 3611169682695799175L;

    private String senderId;        // 发送者的用户id
    private String receiverId;        // 接受者的用户id
    private String msg;                // 聊天内容
    private String msgId;            // 用于消息的签收
}
