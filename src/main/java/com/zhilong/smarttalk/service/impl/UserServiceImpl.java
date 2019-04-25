package com.zhilong.smarttalk.service.impl;

import com.zhilong.smarttalk.enums.MsgActionEnum;
import com.zhilong.smarttalk.enums.MsgSignFlagEnum;
import com.zhilong.smarttalk.enums.SearchFriendsStatusEnum;
import com.zhilong.smarttalk.mapper.*;
import com.zhilong.smarttalk.netty.ChatMsg;
import com.zhilong.smarttalk.netty.DataContent;
import com.zhilong.smarttalk.netty.UserChannelRel;
import com.zhilong.smarttalk.pojo.FriendList;
import com.zhilong.smarttalk.pojo.FriendRequest;
import com.zhilong.smarttalk.pojo.User;
import com.zhilong.smarttalk.pojo.vo.FriendRequestVO;
import com.zhilong.smarttalk.pojo.vo.MyFriendsVO;
import com.zhilong.smarttalk.service.UserService;
import com.zhilong.smarttalk.utils.FastDFSClient;
import com.zhilong.smarttalk.utils.FileUtils;
import com.zhilong.smarttalk.utils.JsonUtils;
import com.zhilong.smarttalk.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FriendListMapper friendListMapper;

    @Autowired
    private FriendRequestMapper friendsRequestMapper;

    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Autowired
    private ChatMsgMapper chatMsgMapper;

    @Autowired
    private Sid sid;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {

        User user = new User();
        user.setUsername(username);
        User result = userMapper.selectOne(user);

        return result != null;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public User queryUserForLogin(String username, String pwd) {

        Example userExample = new Example(User.class);
        Example.Criteria criteria = userExample.createCriteria();

        criteria.andEqualTo("username", username);
        criteria.andEqualTo("password", pwd);

        User result = userMapper.selectOneByExample(userExample);

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public User saveUser(User user) {

        String userId = sid.nextShort();
        // 为每个用户生成一个唯一的二维码
        // String qrCodePath = "/tmp/user" + userId + "qrcode.png";
        // String qrCodePath = "D:/note/user" + userId + "qrcode.png";
        String qrCodePath = "/usr/faceimg/user" + userId + "qrcode.png";
        // smarttalk_qrcode:[username]
        qrCodeUtils.createQRCode(qrCodePath, "smarttalk_qrcode:" + user.getUsername());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);

        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);
        user.setId(userId);
        userMapper.insert(user);

        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public boolean updateUserInfo(User user) {
        // 根据对象主键更新（对象中有值的才更新，不加selecttive会全更新）
        return (userMapper.updateByPrimaryKeySelective(user) == 0 ? false : true);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public User queryUserById(String userId) {
        return userMapper.selectByPrimaryKey(userId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {

        User user = queryUserInfoByUsername(friendUsername);

        // 1. 搜索的用户如果不存在，返回[无此用户]
        if (user == null) {
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }

        // 2. 搜索账号是你自己，返回[不能添加自己]
        if (user.getId().equals(myUserId)) {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }

        // 3. 搜索的朋友已经是你的好友，返回[该用户已经是你的好友]
        Example mfe = new Example(FriendList.class);
        Example.Criteria mfc = mfe.createCriteria();
        mfc.andEqualTo("myUserId", myUserId);
        mfc.andEqualTo("myFriendUserId", user.getId());
        FriendList myFriendsRelation = friendListMapper.selectOneByExample(mfe);
        if (myFriendsRelation != null) {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }

        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public User queryUserInfoByUsername(String username) {
        Example ue = new Example(User.class);
        Example.Criteria uc = ue.createCriteria();
        uc.andEqualTo("username", username);
        return userMapper.selectOneByExample(ue);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {

        // 根据用户名把朋友信息查询出来
        User friend = queryUserInfoByUsername(friendUsername);

        // step 1. 查询发送好友请求记录表
        Example fre = new Example(FriendRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", myUserId);
        frc.andEqualTo("acceptUserId", friend.getId());
        FriendRequest friendRequest = friendsRequestMapper.selectOneByExample(fre);

        if (friendRequest == null) {
            // step 2. 如果不是你的好友，并且好友记录没有添加，则新增好友请求记录
            String requestId = sid.nextShort();

            FriendRequest request = new FriendRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request);
        }
    }

    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example fre = new Example(FriendRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", sendUserId);
        frc.andEqualTo("acceptUserId", acceptUserId);
        friendsRequestMapper.deleteByExample(fre);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId, acceptUserId);
        saveFriends(acceptUserId, sendUserId);
        deleteFriendRequest(sendUserId, acceptUserId);

        Channel sendChannel = UserChannelRel.get(sendUserId);
        if (sendChannel != null) {
            // 使用websocket主动推送消息到请求发起者，更新他的通讯录列表为最新
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type); // action: 重新拉取好友列表
            sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));
        }
    }

    private void saveFriends(String sendUserId, String acceptUserId) {
        FriendList myFriends = new FriendList();
        String recordId = sid.nextShort();
        myFriends.setId(recordId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriends.setMyUserId(sendUserId);
        friendListMapper.insert(myFriends);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFirends = usersMapperCustom.queryMyFriends(userId);
        return myFirends;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {

        com.zhilong.smarttalk.pojo.ChatMsg  msgDB = new com.zhilong.smarttalk.pojo.ChatMsg ();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type); // 未签收
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);
        return msgId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }

    @Override
    public List<com.zhilong.smarttalk.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {

        Example chatExample = new Example(com.zhilong.smarttalk.pojo.ChatMsg.class);
        Example.Criteria chatCriteria = chatExample.createCriteria();
        chatCriteria.andEqualTo("signFlag", 0); // 未签收
        chatCriteria.andEqualTo("acceptUserId", acceptUserId);  // 消息接收者(自己)

        List<com.zhilong.smarttalk.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);
        return result;
    }
}
