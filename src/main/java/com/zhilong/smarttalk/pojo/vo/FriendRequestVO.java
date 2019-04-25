package com.zhilong.smarttalk.pojo.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 好友请求发送方的信息
 */
@Setter
@Getter
public class FriendRequestVO {
    private String sendUserId;
    private String sendUsername;
    private String sendFaceImage;
    private String sendNickname;
}