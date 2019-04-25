package com.zhilong.smarttalk.service;

import com.zhilong.smarttalk.netty.ChatMsg;
import com.zhilong.smarttalk.pojo.User;
import com.zhilong.smarttalk.pojo.vo.FriendRequestVO;
import com.zhilong.smarttalk.pojo.vo.MyFriendsVO;

import java.util.List;

public interface UserService {

    /**
     * @Description: 判断用户名是否存在
     */
    boolean queryUsernameIsExist(String username);

    /**
     * @Description: 查询用户是否存在
     */
    User queryUserForLogin(String username, String pwd);

    /**
     * @Description: 用户注册
     */
    User saveUser(User user);

    /**
     * @Description: 修改用户记录
     */
    boolean updateUserInfo(User user);

    User queryUserById(String userId);

    /**
     * @Description: 搜索朋友的前置条件
     */
    Integer preconditionSearchFriends(String myUserId, String friendUsername);

    /**
     * @Description: 根据用户名查询用户对象
     */
    User queryUserInfoByUsername(String username);

    /**
     * @Description: 添加好友请求记录，保存到数据库
     */
    void sendFriendRequest(String myUserId, String friendUsername);

    /**
     * @Description: 查询好友请求
     */
    List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    /**
     * @Description: 删除好友请求记录
     */
    void deleteFriendRequest(String sendUserId, String acceptUserId);

    /**
     * @Description: 通过好友请求
     * 1. 保存好友
     * 2. 逆向保存好友
     * 3. 删除好友请求记录
     */
    void passFriendRequest(String sendUserId, String acceptUserId);

    /**
     * @Description: 查询好友列表
     */
    List<MyFriendsVO> queryMyFriends(String userId);

    /**
     * @Description: 保存聊天消息到数据库
     */
    String saveMsg(ChatMsg chatMsg);

    /**
     * @Description: 批量签收消息
     */
    void updateMsgSigned(List<String> msgIdList);

    /**
     * @Description: 获取未签收消息列表
     */
    List<com.zhilong.smarttalk.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);

}
