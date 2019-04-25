package com.zhilong.smarttalk.mapper;

import com.zhilong.smarttalk.pojo.User;
import com.zhilong.smarttalk.pojo.vo.FriendRequestVO;
import com.zhilong.smarttalk.pojo.vo.MyFriendsVO;
import com.zhilong.smarttalk.utils.MyMapper;

import java.util.List;


public interface UsersMapperCustom extends MyMapper<User> {

    List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    List<MyFriendsVO> queryMyFriends(String userId);

    void batchUpdateMsgSigned(List<String> msgIdList);

}