package me.xpyex.plugin.allinone.module;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.xpyex.plugin.allinone.api.CommandMenu;
import me.xpyex.plugin.allinone.api.MessageBuilder;
import me.xpyex.plugin.allinone.core.command.argument.ArgParser;
import me.xpyex.plugin.allinone.core.command.argument.GroupParser;
import me.xpyex.plugin.allinone.core.command.argument.StrParser;
import me.xpyex.plugin.allinone.core.command.argument.UserParser;
import me.xpyex.plugin.allinone.core.module.Module;
import me.xpyex.plugin.allinone.modulecode.git.GitInfo;
import me.xpyex.plugin.allinone.modulecode.git.ReleasesUpdate;
import me.xpyex.plugin.allinone.utils.FileUtil;
import me.xpyex.plugin.allinone.utils.MsgUtil;
import me.xpyex.plugin.allinone.utils.Util;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.utils.ExternalResource;

public class GitUpdates extends Module {
    private File urls;

    @Override
    public void register() throws Throwable {
        urls = new File(getDataFolder(), "urls.json");
        if (!urls.exists()) {
            urls.createNewFile();
            FileUtil.writeFile(urls, JSONUtil.toJsonPrettyStr(new ReleasesUpdate()));
        }
        JSONObject i = JSONUtil.parseObj(FileUtil.readFile(urls));
        ReleasesUpdate.setInstance(JSONUtil.toBean(i, ReleasesUpdate.class));

        registerCommand(Contact.class, (source, sender, label, args) -> {
            if (!sender.hasPerm(getName() + ".use", MemberPermission.ADMINISTRATOR)) {
                source.sendMessage("你没有权限");
                return;
            }
            if (args.length == 0) {
                new CommandMenu(label)
                    .add("add <GitHub|Gitee> <Owner/RepoName>", "订阅指定Git平台的仓库，发布releases时推送")
                    .add("remove <Owner/RepoName>", "解除订阅")
                    .send(source);
                return;
            }
            if ("checkNow".equalsIgnoreCase(args[0])) {
                checkUpdate();
            }
            if ("add".equalsIgnoreCase(args[0])) {
                if (args.length < 3) {  //updates add GitHub Owner/RepoName
                    source.sendMessage("参数不足");
                    return;
                }
                if (!args[2].contains("/")) {
                    source.sendMessage("需要填入 <用户名/仓库名> 的格式");
                    return;
                }
                Map<Long, Set<GitInfo>> map;
                if (source.getContact() instanceof Group) {
                    map = ReleasesUpdate.getInstance().getGroups();
                } else {
                    map = ReleasesUpdate.getInstance().getUsers();
                }
                if (!map.containsKey(source.getId())) {
                    map.put(source.getId(), new HashSet<>());
                }
                for (GitInfo.SupportedGits value : GitInfo.SupportedGits.values()) {
                    if (value.toString().equalsIgnoreCase(args[1])) {
                        map.get(source.getId()).add(new GitInfo(value, args[2]));
                        FileUtil.writeFile(urls, JSONUtil.toJsonPrettyStr(ReleasesUpdate.getInstance()));
                        source.sendMessage("已订阅该Repo");
                        return;
                    }
                }
                source.sendMessage("无效的仓库类型");
            }
            if ("remove".equalsIgnoreCase(args[0])) {
                ArgParser.of(StrParser.class).parse(() -> args[1])
                    .ifPresentOrElse(path -> {
                        boolean[] result = {false};
                        ReleasesUpdate.getInstance().getUsers().forEach((ID, info) -> {
                            if (result[0]) return;
                            for (GitInfo gitInfo : info) {
                                if (gitInfo.getRepo().equals(path)) {
                                    runTaskLater(() -> {
                                        ReleasesUpdate.getInstance().getUsers().get(ID).remove(gitInfo);
                                    }, 1);
                                    result[0] = true;
                                    return;
                                }
                            }
                        });
                        ReleasesUpdate.getInstance().getGroups().forEach((ID, info) -> {
                            if (result[0]) return;
                            for (GitInfo gitInfo : info) {
                                if (gitInfo.getRepo().equals(path)) {
                                    runTaskLater(() -> {
                                        ReleasesUpdate.getInstance().getGroups().get(ID).remove(gitInfo);
                                    }, 1);
                                    result[0] = true;
                                    return;
                                }
                            }
                        });
                        source.sendMessage(result[0] ? "已解除订阅" : "未订阅该Repo");
                    }, () -> source.sendMessage("参数不足"));
            }
        }, "updates", "gitUpdates", "git", "repo");
        runTaskTimer(this::checkUpdate, 25 * 60L, 60L);
    }

    private void checkUpdate() throws IOException {
        HashMap<String, String> repoURLs = new HashMap<>();  //Repo, URL
        ReleasesUpdate.getInstance().getGroups().values().forEach(URLs -> {
            URLs.forEach(info -> {
                switch (info.getType()) {
                    case Gitee:
                        repoURLs.put(info.getRepo(), "https://gitee.com/api/v5/repos/" + info.getRepo() + "/releases/latest");
                        break;
                    case GitHub:
                        repoURLs.put(info.getRepo(), "https://api.github.com/repos/" + info.getRepo() + "/releases/latest");
                        break;
                }
            });
        });
        ReleasesUpdate.getInstance().getUsers().values().forEach(URLs -> {
            URLs.forEach(info -> {
                switch (info.getType()) {
                    case Gitee:
                        repoURLs.put(info.getRepo(), "https://gitee.com/api/v5/repos/" + info.getRepo() + "/releases/latest");
                        break;
                    case GitHub:
                        repoURLs.put(info.getRepo(), "https://api.github.com/repos/" + info.getRepo() + "/releases/latest");
                        break;
                }
            });
        });

        HashMap<String, JSONObject> results = new HashMap<>();  //Repo, Results
        repoURLs.forEach((repo, api) -> {
            JSONObject result = new JSONObject(info(HttpRequest.get(api).execute().body()));
            if (!ReleasesUpdate.getInstance().getCache().containsKey(repo) || !ReleasesUpdate.getInstance().getCache().get(repo).equalsIgnoreCase(result.getStr("tag_name"))) {
                //此时就是检查到相对自身而言的“新版本”
                results.put(repo, result);
                //是新版本再存入
            }
        });

        HashMap<Contact, ArrayList<String>> contacts = new HashMap<>();  //Contact, Repo
        ReleasesUpdate.getInstance().getGroups().forEach((ID, URLs) -> {
            ArgParser.of(GroupParser.class).parse(ID + "", Group.class).ifPresent(group -> {
                for (GitInfo info : URLs) {
                    if (!contacts.containsKey(group)) {
                        contacts.put(group, new ArrayList<>());
                    }
                    contacts.get(group).add(info.getRepo());
                }
            });
        });
        ReleasesUpdate.getInstance().getUsers().forEach((ID, URLs) -> {
            ArgParser.of(UserParser.class).parse(ID + "", Friend.class).ifPresent(friend -> {
                for (GitInfo info : URLs) {
                    if (!contacts.containsKey(friend)) {
                        contacts.put(friend, new ArrayList<>());
                    }
                    contacts.get(friend).add(info.getRepo());
                }
            });
        });

        File cacheFolder = new File(getDataFolder(), "cache");
        cacheFolder.mkdirs();
        contacts.forEach((contact, list) -> {
            for (String repo : list) {
                Optional.ofNullable(results.get(repo)).ifPresent(got -> {
                    if (!ReleasesUpdate.getInstance().getCache().containsKey(repo) || !ReleasesUpdate.getInstance().getCache().get(repo).equalsIgnoreCase(got.getStr("tag_name"))) {
                        //此时就是检查到相对自身而言的“新版本”
                        ReleasesUpdate.getInstance().getCache().put(repo, got.getStr("tag_name"));
                        ForwardMessageBuilder builder = MsgUtil.getForwardMsgBuilder(Util.getBot().getAsFriend());
                        String releasePage = got.containsKey("html_url") ? got.getStr("html_url") : "https://gitee.com/" + repo + "/releases";
                        builder.add(Util.getBot(), new PlainText(new MessageBuilder()
                                                                     .plus(repo.split("/")[1] + " 发布了新Release:")
                                                                     .plus("版本名: " + got.getStr("name"))
                                                                     .plus("版本号: " + got.getStr("tag_name"))
                                                                     .plus("发布时间: " + got.getStr("created_at").replace("T", " ").replace("Z", ""))
                                                                     .plus("")
                                                                     .plus("更新内容: ")
                                                                     .plus(got.getStr("body").substring(0, Math.min(2000, got.getStr("body").length())))
                                                                     .plus("")
                                                                     .plus("详细内容请至 <发布页面> 查看")
                                                                     .plus("")
                                                                     .plus("发布页面: " + releasePage)
                                                                     .toString()));
                        contact.sendMessage(builder.build());

                        if (contact instanceof Group) {
                            try {
                                File cacheFile = new File(cacheFolder, got.getJSONArray("assets").getJSONObject(0).getStr("name"));
                                if (!cacheFile.exists()) {
                                    URLConnection connection = new URL(got.getJSONArray("assets").getJSONObject(0).getStr("browser_download_url")).openConnection();
                                    connection.connect();
                                    Files.copy(connection.getInputStream(), cacheFile.toPath());
                                }
                                try (ExternalResource file = ExternalResource.create(cacheFile)) {
                                    ((Group) contact).getFiles().uploadNewFile("RepoUpdates/" + cacheFile.getName(), file);
                                }
                            } catch (IOException e) {
                                handleException(e);
                            }
                        }
                    }
                });
            }
        });
        FileUtil.writeFile(urls, JSONUtil.toJsonPrettyStr(ReleasesUpdate.getInstance()));
        cacheFolder.delete();
    }
}
