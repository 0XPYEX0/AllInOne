package me.xpyex.plugin.allinone.utils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import me.xpyex.plugin.allinone.Main;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.utils.ExternalResource;

public class BilibiliUtil {
    public static Message getVideoInfo(Map<String, Object> param) throws Exception {
        String result = HttpUtil.get("https://api.bilibili.com/x/web-interface/view", param);
        int failCount = 0;
        while (result == null || result.isEmpty()) {
            if (failCount > 5) {
                return new PlainText("解析超时");
            }
            result = HttpUtil.post("https://api.bilibili.com/x/web-interface/view", param);
            failCount++;
            Thread.sleep(5000L);
        }
        Main.LOGGER.info(result);
        JSONObject infos = new JSONObject(result);
        int failed = infos.getInt("code");
        if (failed != 0) {
            String Reason;
            if (failed == -400) {
                Reason = "请求错误";
            } else if (failed == -403) {
                Reason = "权限不足";
            } else if (failed == -404) {
                Reason = "视频不存在";
            } else if (failed == 62002) {
                Reason = "视频不可见(被锁定)";
            } else {
                Reason = "未知原因";
            }
            return new PlainText("解析失败: " + Reason
                    + "\n错误码: " + failed
                    + "\n错误信息: " + infos.getStr("message"));
        }
        JSONObject data = infos.getJSONObject("data");
        int AvID = data.getInt("aid");
        String BvID = data.getStr("bvid");
        int videoCount = data.getInt("videos");
        String title = data.getStr("title");
        String description = data.getStr("desc");
        JSONObject ownerInfo = data.getJSONObject("owner");
        String ownerName = ownerInfo.getStr("name");
        int ownerId = ownerInfo.getInt("mid");
        String faceUrl = data.getStr("pic");

        String videoID = param.containsKey("aid") ? "AV" + param.get("aid") : "BV" + param.get("bvid");
        return new PlainText("视频: " + videoID)
                .plus(Util.getBot().getFriend(1723275529L).uploadImage(Util.getImage(faceUrl)))
                .plus("\nAV号: AV" + AvID
                + "\nBV号: " + BvID
                + "\n标题: " + title
                + "\n简介: " + description
                + "\n分P数: " + videoCount
                + "\n播放地址:\nhttps://bilibili.com/video/av" + AvID + "\nhttps://bilibili.com/video/" + BvID + "\n"
                + "\n作者: " + ownerName
                + "\n作者主页: https://space.bilibili.com/" + ownerId);
    }

    public static Message getUserInfo(int userID) throws Exception {
        Map<String, Object> param = new HashMap<>();
        param.put("mid", userID);
        String result = HttpUtil.get("https://api.bilibili.com/x/space/acc/info", param);
        int failCount = 0;
        while (result == null || result.isEmpty()) {
            if (failCount > 5) {
                return new PlainText("解析超时");
            }
            result = HttpUtil.post("https://api.bilibili.com/x/space/acc/info", param);
            failCount++;
            Thread.sleep(5000L);
        }

        Main.LOGGER.info(result);
        JSONObject infos = new JSONObject(result);
        int success = infos.getInt("code");
        if (success != 0) {
            String Reason;
            if (success == -400) {
                Reason = "请求错误";
            } else {
                Reason = "未知原因";
            }
            return new PlainText("查找用户失败: " + Reason
                    + "\n错误码: " + success
                    + "\n错误信息: " + infos.getStr("message"));
        }
        JSONObject data = infos.getJSONObject("data");
        String gender = data.getStr("sex");  //性别
        String name = data.getStr("name");  //昵称
        String faceURL = data.getStr("face");
        int level = data.getInt("level");  //等级
        int vip = data.getJSONObject("vip").getInt("type");  //0无，1月度，2年度+
        int official = data.getJSONObject("official").getInt("role");  //0无;1,2,7个人认证;3,4,5,6机构认证
        String officialInfo;
        switch (official) {
            case 1:
            case 2:
            case 7:
                officialInfo = "个人认证: " + data.getJSONObject("official").getStr("title");
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                officialInfo = "企业认证: " + data.getJSONObject("official").getStr("title");
                break;
            default:
                officialInfo = "无";
                break;
        }
        String vipInfo;
        switch (vip) {
            case 1:
                vipInfo = "月度大会员";
                break;
            case 2:
                switch (data.getJSONObject("vip").getJSONObject("label").getStr("label_theme")) {
                    case "annual_vip":
                        vipInfo = "年度大会员";
                        break;
                    case "ten_annual_vip":
                        vipInfo = "十年大会员";
                        break;
                    case "hundred_annual_vip":
                        vipInfo = "百年大会员";
                        break;
                    default:
                        vipInfo = "年度大会员";
                        break;
                }
                break;
            default:
                vipInfo = "非大会员";
        }
        return new PlainText("用户: " + userID + "\n" +
                "昵称: " + name + "\n")
                .plus(Util.getBot().getFriend(1723275529L).uploadImage(Util.getImage(faceURL)))
                .plus("性别: " + gender + "\n" +
                        "等级: LV" + level + "\n" +
                        "会员: " + vipInfo + "\n" +
                        "认证信息: " + officialInfo + "\n" +
                        "空间地址: https://space.bilibili.com/" + userID);
    }
}
