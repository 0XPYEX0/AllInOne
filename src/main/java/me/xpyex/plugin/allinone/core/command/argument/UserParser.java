package me.xpyex.plugin.allinone.core.command.argument;

import java.util.Optional;
import me.xpyex.plugin.allinone.utils.Util;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.contact.Stranger;

public class UserParser extends ArgParser {
    public Optional<?> parse(String arg) {
        long id = Long.parseLong(arg.replaceAll("[^0-9]", ""));
        Friend friend = Util.getBot().getFriend(id);  //加过的好友
        if (friend != null) {
            return Optional.of(friend);
        }
        Stranger stranger = Util.getBot().getStranger(id);  //临时对话过的
        if (stranger != null) {
            return Optional.of(stranger);
        }

        if (getParseObj() != null && getParseObj() instanceof Member) {  //触发命令如果是群，从群里找看看
            NormalMember member = ((Member) getParseObj()).getGroup().get(id);
            if (member != null) {
                return Optional.of(member);
            }
        }

        for (Group group : Util.getBot().getGroups()) {  //在bot加过的群里找看看
            NormalMember member = group.get(id);
            if (member != null) {
                return Optional.of(member);
            }
        }

        return Optional.empty();  //bot从未接触过的陌生人，或不存在
    }
}
