package me.xpyex.plugin.allinone.module;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.WeakHashMap;
import me.xpyex.plugin.allinone.api.CommandMenu;
import me.xpyex.plugin.allinone.api.MessageBuilder;
import me.xpyex.plugin.allinone.core.command.CommandExecutor;
import me.xpyex.plugin.allinone.core.command.argument.ArgParser;
import me.xpyex.plugin.allinone.core.command.argument.GroupParser;
import me.xpyex.plugin.allinone.core.command.argument.StrParser;
import me.xpyex.plugin.allinone.core.mirai.ContactTarget;
import me.xpyex.plugin.allinone.core.module.Module;
import me.xpyex.plugin.allinone.modulecode.chatgpt.ChatMessage;
import me.xpyex.plugin.allinone.utils.StringUtil;
import me.xpyex.plugin.allinone.utils.Util;
import me.xpyex.plugin.allinone.utils.ValueUtil;
import net.mamoe.mirai.Mirai;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;
import net.mamoe.mirai.message.data.PlainText;

public final class ChatGPT extends Module {
    private static final WeakHashMap<Long, ChatMessage> CHAT_CACHE = new WeakHashMap<>();
    private static final String DEFAULT_MSG = "";
    private static final String API_VER3 = "";
    private static final String API_KEY3 = "";
    private static final String API_VER4 = "";
    private static final String API_KEY4 = "";
    private static final String DENIED_MSG_3 = "你没有使用 ChatGPT 3.5 模型的权限";
    private static final String DENIED_MSG_4 = "你没有使用 ChatGPT 4 模型的权限";
    private static final HashMap<Long, String> GROUP_RULES = new HashMap<>();

    @Override
    public void register() throws Throwable {
        registerCommand(Contact.class, new CommandExecutor<>() {
            @Override
            public void execute(ContactTarget<Contact> source, ContactTarget<User> sender, String label, String[] args) {
                if (!sender.hasPerm("ChatGPT.use", MemberPermission.ADMINISTRATOR)) {
                    source.sendMessage("你没有权限");
                    return;
                }
                if (args.length == 0) {
                    new CommandMenu(label)
                        .add("talk <Messages>...", "与ChatGPT对话，每次对话保留5回合")
                        .add("reset", "开启新话题")
                        .add("reGo", "按照先前的话题重新生成")
                        .add("groupRules")
                        .send(source);
                    return;
                }
                if ("reset".equalsIgnoreCase(args[0])) {
                    CHAT_CACHE.remove(sender.getId());
                    source.sendMessage("已清除连续对话记忆");
                    return;
                }
                if ("groupRules".equalsIgnoreCase(args[0])) {
                    if (!sender.hasPerm("ChatGPT.setGroupPerm", MemberPermission.ADMINISTRATOR)) {
                        source.sendMessage("不理你不理你！");
                        return;
                    }
                    ArgParser.of(GroupParser.class).parse(() -> args[1], Group.class).ifPresentOrElse(group -> {
                        ArgParser.of(StrParser.class).parse(() -> String.join(" ", Arrays.copyOfRange(args, 2, args.length)), String.class).ifPresentOrElse(rule -> {
                            try {
                                Files.writeString(new File(getDataFolder(), group.getId() + ".txt").toPath(), rule, StandardCharsets.UTF_8);
                                GROUP_RULES.put(group.getId(), rule);
                                source.sendMessage("已保存规则");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, () -> source.sendMessage("未输入具体规则"));
                    }, () -> source.sendMessage("未输入群号"));
                    return;
                }
                if (StringUtil.equalsIgnoreCaseOr(args[0], "talk", "talk4")) {
                    boolean is3 = "talk".equalsIgnoreCase(args[0]);
                    if (is3 && !sender.hasPerm("ChatGPT.use.3", MemberPermission.ADMINISTRATOR)) {
                        source.sendMessage(DENIED_MSG_3);
                        return;
                    }
                    if (!is3 && !sender.hasPerm("ChatGPT.use.4")) {  //调用GPT4且无使用权限，则拦截
                        source.sendMessage(DENIED_MSG_4);
                        return;
                    }
                    if (args.length == 1) {
                        source.sendMessage("你想聊点什么？😊");
                        return;
                    }
                    if (source.isGroup() && source.getContactAsGroup().getBotPermission().getLevel() > sender.getContactAsMember().getPermission().getLevel()) {
                        Mirai.getInstance().recallMessage(Util.getBot(), getEvent(source).getSource());
                    }
                    ValueUtil.ifNull(CHAT_CACHE.get(sender.getId()), () -> {  //若还没有聊过天，则新建缓存
                        CHAT_CACHE.put(sender.getId(), ChatMessage.of(ChatMessage.Role.SYSTEM, GROUP_RULES.getOrDefault(source.getId(), DEFAULT_MSG)));
                    });
                    String userMsg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));  //拼接除了talk以外剩下的参数

                    ChatMessage chatMessage = CHAT_CACHE.get(sender.getId());  //获取其缓存
                    chatMessage.plus(ChatMessage.Role.USER, userMsg);

                    ForwardMessageBuilder builder = new ForwardMessageBuilder(source.getContact());
                    for (int i = 1; i < chatMessage.getMessage().size(); i++) {
                        JSONObject obj = chatMessage.getMessage().getJSONObject(i);
                        builder.add("user".equalsIgnoreCase(obj.getStr("role")) ? sender.getContact() : Util.getBot(), new PlainText(obj.getStr("content")));
                    }
                    builder.add(Util.getBot(), new PlainText(talkToGPT(sender.getId(), is3 ? API_VER3 : API_VER4, is3 ? API_KEY3 : API_KEY4)));
                    source.sendMessage(builder.build());
                    return;
                }
                if (StringUtil.equalsIgnoreCaseOr(args[0], "reGo", "reGo4")) {  //重新生成
                    boolean is3 = "reGo".equalsIgnoreCase(args[0]);
                    if (is3 && !sender.hasPerm("ChatGPT.use.3", MemberPermission.ADMINISTRATOR)) {
                        source.sendMessage(DENIED_MSG_3);
                        return;
                    }
                    if (!is3 && !sender.hasPerm("ChatGPT.use.4")) {  //调用GPT4且无使用权限，则拦截
                        source.sendMessage(DENIED_MSG_4);
                        return;
                    }
                    if (!CHAT_CACHE.containsKey(sender.getId())) {
                        source.sendMessage("抱歉，我已经遗忘了与您的对话...");
                        return;
                    }
                    ChatMessage chatMessage = CHAT_CACHE.get(sender.getId());  //获取其缓存
                    chatMessage.getMessage().remove(chatMessage.getMessage().size() - 1);  //清除最终的缓存

                    ForwardMessageBuilder builder = new ForwardMessageBuilder(source.getContact());
                    for (int i = 1; i < chatMessage.getMessage().size(); i++) {
                        JSONObject obj = chatMessage.getMessage().getJSONObject(i);
                        builder.add("user".equalsIgnoreCase(obj.getStr("role")) ? sender.getContact() : Util.getBot(), new PlainText(obj.getStr("content")));
                    }
                    builder.add(Util.getBot(), new PlainText(talkToGPT(sender.getId(), is3 ? API_VER3 : API_VER4, is3 ? API_KEY3 : API_KEY4)));
                    source.sendMessage(builder.build());
                    return;
                }
                new MessageBuilder("未知的参数").plus("执行 #" + label).plus("查看帮助").send(source);
            }
        }, "ChatGPT", "GPT", "Chat", "ChatBot");

        for (File file : getDataFolder().listFiles()) {
            GROUP_RULES.put(Long.parseLong(file.getName().split("\\.")[0]), Files.readString(file.toPath(), Charset.defaultCharset()));
        }
    }

    private String talkToGPT(long id, String apiHost, String apiKey) {
        ValueUtil.ifNull(CHAT_CACHE.get(id), () -> {  //若还没有聊过天，则新建缓存
            CHAT_CACHE.put(id, ChatMessage.of(ChatMessage.Role.SYSTEM, GROUP_RULES.getOrDefault(id, DEFAULT_MSG)));
        });
        try {
            JSONObject outBody = new JSONObject()  //主要的Body参数
                                     .set("temperature", 0.7)
                                     .set("top_p", 0.95)
                                     .set("frequency_penalty", 0)
                                     .set("presence_penalty", 0)
                                     .set("max_tokens", 800)
                                     .set("stop", JSONNull.NULL);
            ChatMessage chatMessage = CHAT_CACHE.get(id);  //获取其缓存

            while (chatMessage.getMessage().size() >= 11) {  //只保留5回合，第一条为System
                chatMessage.getMessage().remove(1);  //0是System语句，无需移除。从1开始是对话语句
            }

            outBody.set("messages", chatMessage.getMessage());

            String result = HttpUtil.createPost(apiHost)
                                .header("Content-Type", "application/json")
                                .header("api-key", apiKey)
                                .body(info(outBody.toString()))
                                .execute()
                                .body();
            @SuppressWarnings("all")
            JSONObject jsonResult = new JSONObject(info(result));
            if (jsonResult.containsKey("error")) {
                chatMessage.getMessage().remove(chatMessage.getMessage().size() - 1);
                return new MessageBuilder().plus("GPT拒绝回答: " + jsonResult.getJSONObject("error").getStr("message"))
                           .plus("")
                           .plus("你触犯的规则类别: " + jsonResult.getJSONObject("error").getJSONObject("innererror").getStr("code"))
                           .plus("")
                           .plus("你的上一个提问已被清除")
                           .toString();
            }
            String gptSaid = jsonResult.getJSONArray("choices")
                                 .getJSONObject(0)
                                 .getJSONObject("message")
                                 .getStr("content");
            if (gptSaid.trim().endsWith("<STOP_HERE>")) {
                CHAT_CACHE.remove(id);
                return gptSaid.replace("<STOP_HERE>", "\n\n我想我们需要换个新话题了\n先前的对话记录已清除");
            }
            chatMessage.plus(ChatMessage.Role.ASSISTANT, gptSaid);
            return gptSaid;
        } catch (IORuntimeException e) {
            handleException(e);
            return "网络异常，访问失败: " + e;
        }
    }
}
