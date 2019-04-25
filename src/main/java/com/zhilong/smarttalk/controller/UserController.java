package com.zhilong.smarttalk.controller;

import com.zhilong.smarttalk.enums.OperatorFriendRequestTypeEnum;
import com.zhilong.smarttalk.enums.SearchFriendsStatusEnum;
import com.zhilong.smarttalk.pojo.ChatMsg;
import com.zhilong.smarttalk.pojo.User;
import com.zhilong.smarttalk.pojo.bo.UsersBO;
import com.zhilong.smarttalk.pojo.vo.MyFriendsVO;
import com.zhilong.smarttalk.pojo.vo.UsersVO;
import com.zhilong.smarttalk.service.UserService;
import com.zhilong.smarttalk.utils.FastDFSClient;
import com.zhilong.smarttalk.utils.FileUtils;
import com.zhilong.smarttalk.utils.JSONResult;
import com.zhilong.smarttalk.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("u")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;

    /**
     * @Description: 用户注册/登录
     */
    @PostMapping("/registOrLogin")
    public JSONResult registOrLogin(@RequestBody User user) throws Exception {

        // 0. 判断用户名和密码不能为空
        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())) {
            return JSONResult.errorMsg("用户名或密码不能为空...");
        }

        // 1. 判断用户名是否存在，如果存在就登录，如果不存在则注册
        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        User userResult = null;
        if (usernameIsExist) {
            // 1.1 登录
            userResult = userService.queryUserForLogin(user.getUsername(), MD5Utils.getMD5Str(user.getPassword()));
            if (userResult == null) {
                return JSONResult.errorMsg("用户名或密码不正确...");
            }
        } else {
            // 1.2 注册
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userService.saveUser(user);
        }

        UsersVO userVO = new UsersVO();
        BeanUtils.copyProperties(userResult, userVO);

        return JSONResult.ok(userVO);
    }

    @PostMapping("/uploadFaceBase64")
    public JSONResult uploadFileBase64(@RequestBody UsersBO usersBO) throws Exception {
        //获取前端传输的base64字符串
        String faceData = usersBO.getFaceData();
        //String userFacePath = "D:/note/" + usersBO.getUserId() + "userface.png";
        String userFacePath = "/usr/faceimg/" + usersBO.getUserId() + "userface.png";
        boolean isupload = FileUtils.base64ToFile(userFacePath, faceData);
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);
        //获取缩略图url
        String thump = "_80x80.";
        String arr[] = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];

        User user = new User();
        user.setId(usersBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);

        boolean b = userService.updateUserInfo(user);
        System.out.println("update status : " + b);
        User result = userService.queryUserById(user.getId());

        return JSONResult.ok(result);
    }

    /**
     * @Description: 设置用户昵称
     */
    @PostMapping("/setNickName")
    public JSONResult setNickname(@RequestBody UsersBO userBO) {

        User user = new User();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());    // 这里没用判断非空，长度等，可以改善

        boolean b = userService.updateUserInfo(user);
        System.out.println("update status : " + b);
        User result = userService.queryUserById(user.getId());

        return JSONResult.ok(result);
    }

    /**
     * @Description: 搜索好友接口, 根据账号做匹配查询而不是模糊查询
     */
    @PostMapping("/search")
    public JSONResult searchUser(String myUserId, String friendUsername) throws Exception {

        // 0. 判断 myUserId friendUsername 不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return JSONResult.errorMsg("");
        }

        // 前置条件 - 1. 搜索的用户如果不存在，返回		[无此用户]
        // 前置条件 - 2. 搜索账号是你自己，返回			[不能添加自己]
        // 前置条件 - 3. 搜索的朋友已经是你的好友，返回	[该用户已经是你的好友]
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            User user = userService.queryUserInfoByUsername(friendUsername);
            UsersVO userVO = new UsersVO();
            BeanUtils.copyProperties(user, userVO);
            return JSONResult.ok(userVO);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return JSONResult.errorMsg(errorMsg);
        }
    }

    @PostMapping("/addFriendRequest")
    public JSONResult addFriendRequest(String myUserId, String friendUsername) {

        // 0. 判断 myUserId friendUsername 不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return JSONResult.errorMsg("");
        }

        // 前置条件 - 1. 搜索的用户如果不存在，返回		[无此用户]
        // 前置条件 - 2. 搜索账号是你自己，返回			[不能添加自己]
        // 前置条件 - 3. 搜索的朋友已经是你的好友，返回	[该用户已经是你的好友]
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendRequest(myUserId, friendUsername);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return JSONResult.errorMsg(errorMsg);
        }

        return JSONResult.ok();
    }

    /**
     * @Description: 发送添加好友的请求
     */
    @PostMapping("/queryFriendRequests")
    public JSONResult queryFriendRequests(String userId) {

        // 0. 判断不能为空
        if (StringUtils.isBlank(userId)) {
            return JSONResult.errorMsg("");
        }

        // 1. 查询用户接受到的朋友申请
        return JSONResult.ok(userService.queryFriendRequestList(userId));
    }

    /**
     * @Description: 接受方 通过或者忽略朋友请求
     */
    @PostMapping("/operFriendRequest")
    public JSONResult operFriendRequest(String acceptUserId, String sendUserId, Integer operType) {

        // 0. acceptUserId sendUserId operType 判断不能为空
        if (StringUtils.isBlank(acceptUserId) || StringUtils.isBlank(sendUserId) || operType == null) {
            return JSONResult.errorMsg("");
        }

        // 1. 如果operType 没有对应的枚举值，则直接抛出空错误信息
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return JSONResult.errorMsg("");
        }

        if (operType == OperatorFriendRequestTypeEnum.IGNORE.type) {
            // 2. 判断如果忽略好友请求，则直接删除好友请求的数据库表记录
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        } else if (operType == OperatorFriendRequestTypeEnum.PASS.type) {
            // 3. 判断如果是通过好友请求，则互相增加好友记录到数据库对应的表
            //	  【#### 然后删除好友请求的数据库表记录 ####】
            userService.passFriendRequest(sendUserId, acceptUserId);
        }

        // 4. 数据库查询好友列表
        List<MyFriendsVO> myFirends = userService.queryMyFriends(acceptUserId);

        return JSONResult.ok(myFirends);
    }

    /**
     * @Description: 查询我的好友列表
     */
    @PostMapping("/myFriends")
    public JSONResult myFriends(String userId) {
        // 0. userId 判断不能为空
        if (StringUtils.isBlank(userId)) {
            return JSONResult.errorMsg("");
        }

        // 1. 数据库查询好友列表
        List<MyFriendsVO> myFirends = userService.queryMyFriends(userId);

        return JSONResult.ok(myFirends);
    }

    /**
     * @Description: 用户手机端获取未签收的消息列表
     */
    @PostMapping("/getUnReadMsgList")
    public JSONResult getUnReadMsgList(String acceptUserId) {

        if (StringUtils.isBlank(acceptUserId)) {
            return JSONResult.errorMsg("");
        }

        List<ChatMsg> unreadMsgList = userService.getUnReadMsgList(acceptUserId);
        return JSONResult.ok(unreadMsgList);
    }
}
