package me.xpyex.plugin.allinone.module.core;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import me.xpyex.plugin.allinone.api.CommandMenu;
import me.xpyex.plugin.allinone.api.MessageBuilder;
import me.xpyex.plugin.allinone.core.command.argument.ArgParser;
import me.xpyex.plugin.allinone.core.command.argument.GroupParser;
import me.xpyex.plugin.allinone.core.module.CoreModule;
import me.xpyex.plugin.allinone.utils.StringUtil;
import me.xpyex.plugin.allinone.utils.Util;
import net.mamoe.mirai.console.MiraiConsole;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent;
import net.mamoe.mirai.event.events.GroupEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.event.events.NewFriendRequestEvent;
import net.mamoe.mirai.event.events.NudgeEvent;
import net.mamoe.mirai.event.events.UserEvent;

@SuppressWarnings("unused")
public class BotManager extends CoreModule {
    private static final ArrayList<String> IGNORED_LIST = new ArrayList<>();
    private static final ArrayList<NewFriendRequestEvent> REQUESTS = new ArrayList<>();

    @Override
    public void register() {
        registerCommand(Contact.class, ((source, sender, label, args) -> {
            if (!sender.hasPerm(getName() + ".use")) {
                source.sendMessage("你没有权限");
                return;
            }


            if (args.length == 0) {  //根帮助
                new CommandMenu(label)
                    .add("group", "群相关操作")
                    .add("friend", "好友相关操作")
                    .add("user", "用户相关操作")
                    .add("end|exit|shutdown|stop", "关闭Bot，自动重启")
                    .send(source);
                return;
            }


            if (args[0].equalsIgnoreCase("group")) {  //群相关
                if (args.length == 1) {
                    CommandMenu menu = new CommandMenu(label + " group")
                                           .add("quit <ID>", "令机器人退出该群")
                                           .add("ignore <ID>", "忽略该群触发的事件")
                                           .add("list", "列出该机器人加入的所有群");
                    menu.send(source);
                    return;
                }
                if (args.length == 2) {
                    if (StringUtil.equalsIgnoreCaseOr(args[1], "quit", "ignore")) {
                        source.sendMessage("参数不足，请填写ID");
                        return;
                    }
                    if (args[1].equalsIgnoreCase("list")) {
                        MessageBuilder messager = new MessageBuilder();
                        messager.plus("机器人加入的群列表: ");
                        for (Group g : Util.getBot().getGroups()) {
                            messager.plus(g.getName() + " (" + g.getId() + ")");
                        }
                        messager.send(source);
                        return;
                    }
                }
                ArgParser.of(GroupParser.class).parse(() -> args[2], Group.class)
                    .ifPresentOrElse(group -> {
                            if (args[1].equalsIgnoreCase("quit")) {
                                source.sendMessage("执行操作: 退出群 " + group.getId());
                                group.quit();
                                return;
                            }
                            if (args[1].equalsIgnoreCase("ignore")) {
                                source.sendMessage("执行操作: 忽略群 " + group.getId());
                                IGNORED_LIST.add("Group-" + group.getId());
                                return;
                            }
                        }, () ->
                               new MessageBuilder()
                                   .plus("群不存在")
                                   .plus("原因可能是: ")
                                   .plus("①群不存在，即群号输入有误")
                                   .plus("②Bot不在指定群内")
                                   .send(source)
                    );
            }

            if (args[0].equalsIgnoreCase("friend")) {  //好友相关
                if (args.length == 1) {
                    CommandMenu menu = new CommandMenu(label + " friend")
                                           .add("delete <ID>", "令机器人删除该好友")
                                           .add("list", "列出该机器人的好友列表");
                    menu.send(source);
                    return;
                }
                if (args.length == 2) {
                    if (StringUtil.equalsIgnoreCaseOr(args[1], "del", "delete", "accept", "deny")) {
                        source.sendMessage("参数不足，请填写ID");
                        return;
                    }
                    if (args[1].equalsIgnoreCase("list")) {
                        MessageBuilder messager = new MessageBuilder();
                        messager.plus("机器人的好友列表: ");
                        for (Friend f : Util.getBot().getFriends()) {
                            messager.plus(f.getNick() + " (" + f.getId() + ")");
                        }
                        messager.send(source);
                        return;
                    }
                }
                if (StringUtil.equalsIgnoreCaseOr(args[1], "accept", "deny")) {
                    try {
                        NewFriendRequestEvent event = REQUESTS.get(Integer.parseInt(args[2]));
                        if ("accept".equalsIgnoreCase(args[1])) {
                            event.accept();
                        } else {
                            event.reject(false);
                        }
                        new MessageBuilder()
                            .plus("已处理编号为 " + args[2] + " 的好友申请")
                            .plus("ID: " + event.getFromId())
                            .plus("Nick: " + event.getFromNick())
                            .plus("Group: " + event.getFromGroupId())
                            .send(source);
                        REQUESTS.remove(Integer.parseInt(args[2]));
                    } catch (NoSuchElementException | NumberFormatException ignored) {
                        source.sendMessage("没有这条申请");
                    }
                    return;
                }
                Friend friend;
                try {
                    friend = Util.getBot().getFriendOrFail(Long.parseLong(args[2]));
                } catch (NumberFormatException ignored) {
                    source.sendMessage("填入的QQ号非整数");
                    return;
                } catch (NoSuchElementException ignored) {
                    source.sendMessage("机器人并非指定QQ的好友，无法操作");
                    return;
                }
                if (StringUtil.equalsIgnoreCaseOr(args[1], "del", "delete")) {
                    //
                    if (PermManager.hasPerm(friend, "BotManager.admin", null)) {
                        source.sendMessage("不允许删除该好友");
                        return;
                    }
                    source.sendMessage("执行操作: 删除好友 " + friend.getId());
                    friend.delete();
                    return;
                }
            }

            if (args[0].equalsIgnoreCase("user")) {  //用户相关
                if (args.length == 1) {
                    new CommandMenu(label + " user")
                        .add("ignore <ID>", "忽略该用户触发的事件")
                        .send(source);
                    return;
                }
                if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("ignore")) {
                        source.sendMessage("参数不足，请填写ID");
                        return;
                    }
                }
                try {
                    long id = Long.parseLong(args[2]);
                    if (args[1].equalsIgnoreCase("ignore")) {
                        if (id == Util.OWNER_ID) {
                            source.sendMessage("不允许屏蔽该用户");
                            return;
                        }
                        source.sendMessage("执行操作: 忽略用户 " + id);
                        IGNORED_LIST.add("User-" + id);
                        return;
                    }
                } catch (NumberFormatException ignored) {
                    source.sendMessage("填入的QQ号非整数");
                    return;
                }
            }

            if (StringUtil.equalsIgnoreCaseOr(args[0], "shutdown", "exit", "stop", "end")) {
                source.sendMessage("开始重启");
                MiraiConsole.shutdown();
                return;
            }

            source.sendMessage("未知子命令，请执行 #" + label + " 查看帮助");
        }), "BotManager", "Bot");

        listenEvent(BotInvitedJoinGroupRequestEvent.class, event -> {
            User user = event.getInvitor();
            String perm = getName() + ".invite";
            //
            if (PermManager.hasPerm(user, perm, null)) {
                event.accept();
            }
        });

        listenEvent(NewFriendRequestEvent.class, event -> {
            REQUESTS.add(event);
            new MessageBuilder()
                .plus(event.getFromNick() + " (" + event.getFromId() + ")")
                .plus("请求添加好友")
                .plus("申请理由: " + event.getMessage())
                .plus("执行")
                .plus("#bot friend accept " + (REQUESTS.size() - 1) + "  同意申请")
                .plus("或")
                .plus("#bot friend deny " + (REQUESTS.size() - 1) + "  拒绝申请")
                .send(Util.getOwner());
        });
    }

    @Override
    public boolean acceptEvent(Event event) {
        if (event instanceof GroupEvent && IGNORED_LIST.contains("Group-" + ((GroupEvent) event).getGroup().getId()))
            return false;

        if (event instanceof UserEvent && IGNORED_LIST.contains("User-" + ((UserEvent) event).getUser().getId()))
            return false;

        if (event instanceof MessageEvent && IGNORED_LIST.contains("User-" + ((MessageEvent) event).getSender().getId()))
            return false;

        if (event instanceof NudgeEvent && (IGNORED_LIST.contains("User-" + ((NudgeEvent) event).getFrom().getId()) || IGNORED_LIST.contains("Group-" + ((NudgeEvent) event).getSubject().getId())))
            return false;
        return true;
    }
}
