package me.xpyex.plugin.allinone.core.command.argument;

import java.util.Optional;
import java.util.WeakHashMap;
import me.xpyex.plugin.allinone.utils.Util;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.contact.Stranger;
import net.mamoe.mirai.contact.User;
import org.jetbrains.annotations.NotNull;

public class UserParser extends ContactParser {
    private static final WeakHashMap<Long, User> CACHE = new WeakHashMap<>();

    @NotNull
    public Optional<?> parse(String arg) {
        String strID = arg.replaceAll("[^0-9]", "");
        if (strID.isEmpty()) return Optional.empty();
        long id = Long.parseLong(strID);

        if (CACHE.containsKey(id)) {
            return Optional.ofNullable(CACHE.get(id));
        }

        Friend friend = Util.getBot().getFriend(id);  //加过的好友
        if (friend != null) {
            CACHE.put(id, friend);
            return Optional.of(friend);
        }
        Stranger stranger = Util.getBot().getStranger(id);  //临时对话过的
        if (stranger != null) {
            CACHE.put(id, stranger);
            return Optional.of(stranger);
        }

        if (getParseObj() != null && getParseObj() instanceof Member) {  //触发命令如果是群，从群里找看看
            NormalMember member = ((Member) getParseObj()).getGroup().get(id);
            if (member != null) {
                CACHE.put(id, member);
                return Optional.of(member);
            }
        }

        for (Group group : Util.getBot().getGroups()) {  //在bot加过的群里找看看
            NormalMember member = group.get(id);
            if (member != null) {
                CACHE.put(id, member);
                return Optional.of(member);
            }
        }

        return Optional.empty();  //bot从未接触过的陌生人，或不存在
    }
}
