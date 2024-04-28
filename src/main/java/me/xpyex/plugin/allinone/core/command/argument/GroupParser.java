package me.xpyex.plugin.allinone.core.command.argument;

import cn.hutool.json.JSONUtil;
import java.util.Optional;
import me.xpyex.plugin.allinone.utils.Util;
import net.mamoe.mirai.contact.Member;
import org.jetbrains.annotations.NotNull;

public class GroupParser extends ContactParser {
    @NotNull
    @Override
    public Optional<?> parse(String arg) {
        if ("this".equalsIgnoreCase(arg)) {
            System.out.println(getParseObj() + "");
            System.out.println(JSONUtil.parse(this).toStringPretty());
            if (getParseObj() instanceof Member) {
                return Optional.of(((Member) getParseObj()).getGroup());
            }
        }
        String strID = arg.replaceAll("[^0-9]", "");
        if (strID.trim().isEmpty()) return Optional.empty();
        return Optional.ofNullable(Util.getBot().getGroup(Long.parseLong(strID)));
    }
}
