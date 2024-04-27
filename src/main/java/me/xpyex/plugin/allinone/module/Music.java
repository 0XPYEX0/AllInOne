/**
 * Mirai Song Plugin
 * Copyright (C) 2021  khjxiaogu
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.xpyex.plugin.allinone.module;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import me.xpyex.plugin.allinone.core.module.Module;
import me.xpyex.plugin.allinone.modulecode.music.MusicCardProvider;
import me.xpyex.plugin.allinone.modulecode.music.MusicInfo;
import me.xpyex.plugin.allinone.modulecode.music.MusicSource;
import me.xpyex.plugin.allinone.modulecode.music.cardprovider.MiraiCardProvider;
import me.xpyex.plugin.allinone.modulecode.music.musicsource.KugouMusicSource;
import me.xpyex.plugin.allinone.modulecode.music.musicsource.NetEaseMusicSource;
import me.xpyex.plugin.allinone.modulecode.music.musicsource.QQMusicSource;
import me.xpyex.plugin.allinone.utils.MsgUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.MemberPermission;

public class Music extends Module {

    /**
     * 命令列表.
     */
    public static final Map<String, BiConsumer<Contact, String[]>> COMMANDS = new ConcurrentHashMap<>();
    /**
     * 音乐来源.
     */
    public static final Map<String, MusicSource> SOURCES = Collections.synchronizedMap(new LinkedHashMap<>());
    /**
     * 外观来源
     */
    public static final Map<String, MusicCardProvider> CARDS = new ConcurrentHashMap<>();
    // 请求音乐的线程池。
    private static final Executor EXEC = Executors.newFixedThreadPool(8);

    static {
        // 注册音乐来源
        SOURCES.put("QQ音乐", new QQMusicSource());
        SOURCES.put("网易", new NetEaseMusicSource());
        SOURCES.put("酷狗", new KugouMusicSource());
        // 注册外观
        CARDS.put("Mirai", new MiraiCardProvider());
    }

    static {
        HttpURLConnection.setFollowRedirects(true);
    }

    /**
     * 使用现有的来源和外观制作指令执行器
     *
     * @param source 音乐来源名称
     * @param card   音乐外观名称
     * @return return 返回一个指令执行器，可以注册到命令列表里面
     */
    public BiConsumer<Contact, String[]> makeTemplate(String source, String card) {
        if (source.equals("all"))
            return makeSearchesTemplate(card);
        MusicCardProvider cb = CARDS.get(card);
        if (cb == null)
            throw new IllegalArgumentException("card template not exists");
        MusicSource mc = SOURCES.get(source);
        if (mc == null)
            throw new IllegalArgumentException("music source not exists");
        return (contact, args) -> {
            String sn = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
            EXEC.execute(() -> {
                MusicInfo mi;
                try {
                    mi = mc.get(sn);
                } catch (Throwable t) {
                    debug(t);
                    MsgUtil.sendMsg(contact, "无法找到歌曲");
                    return;
                }
                try {
                    MsgUtil.sendMsg(contact, cb.process(mi, contact));
                } catch (Throwable t) {
                    debug(t);
                    MsgUtil.sendMsg(contact, "无效的分享");
                }
            });
        };
    }

    /**
     * 自动搜索所有源并且以指定外观返回
     *
     * @param card 音乐外观名称
     * @return return 返回一个指令执行器，可以注册到命令列表里面
     */
    public BiConsumer<Contact, String[]> makeSearchesTemplate(String card) {
        MusicCardProvider cb = CARDS.get(card);
        if (cb == null)
            throw new IllegalArgumentException("card template not exists");
        return (contact, args) -> {
            String sn = String.join(" ", Arrays.copyOfRange(args, 0, args.length));

            EXEC.execute(() -> {
                for (MusicSource mc : SOURCES.values()) {
                    MusicInfo mi;
                    try {
                        mi = mc.get(sn);
                    } catch (Throwable t) {
                        debug(t);
                        continue;
                    }
                    try {
                        MsgUtil.sendMsg(contact, cb.process(mi, contact));
                    } catch (Throwable t) {
                        debug(t);
                        MsgUtil.sendMsg(contact, "无效的分享");
                    }
                    return;
                }
                MsgUtil.sendMsg(contact, "无法找到歌曲");
            });

        };
    }

    @Override
    public void register() {
        COMMANDS.put("点歌", makeTemplate("QQ音乐", "Mirai")); // 标准样板
        COMMANDS.put("网易", makeTemplate("网易", "Mirai"));
        COMMANDS.put("酷狗", makeTemplate("酷狗", "Mirai"));

        registerCommand(Contact.class, ((source, sender, label, args) -> {
            if (sender.hasPerm(getName() + ".use", MemberPermission.ADMINISTRATOR)) {
                BiConsumer<Contact, String[]> exec = COMMANDS.get(label);
                if (exec != null) {
                    exec.accept(source.getContact(), args);
                }
            } else {
                source.sendMessage("你没有权限");
            }
        }), "点歌", "网易", "酷狗");
    }
}