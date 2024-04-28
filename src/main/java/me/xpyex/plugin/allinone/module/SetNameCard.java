package me.xpyex.plugin.allinone.module;

import me.xpyex.plugin.allinone.core.module.Module;
import me.xpyex.plugin.allinone.utils.MsgUtil;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.MemberPermission;

@SuppressWarnings("unused")
public class SetNameCard extends Module {
    private SetNameCard() {
        this.DEFAULT_DISABLED = true;
    }

    @Override
    public void register() {
        registerCommand(Group.class, ((source, sender, label, args) -> {
            if (!sender.hasPerm(getName() + ".use", MemberPermission.ADMINISTRATOR)) {
                MsgUtil.sendMsg(source, "你没有权限");
                return;
            }
            if (args.length == 0) {
                MsgUtil.sendMsg(source, "参数不足");
                return;
            }
            source.getContact().getBotAsMember().setNameCard(String.join(" ", args));
            MsgUtil.sendMsg(source, "已修改");
        }), "setNameCard", "nameCard");
    }
}
