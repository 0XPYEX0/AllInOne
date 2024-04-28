package me.xpyex.plugin.allinone.core.mirai;

import lombok.Getter;
import me.xpyex.plugin.allinone.module.core.PermManager;
import me.xpyex.plugin.allinone.utils.MsgUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.Message;

@Getter
public class ContactTarget<C extends Contact> {
    private final C contact;

    private final long createdTime = System.currentTimeMillis();

    public ContactTarget(C contact) {
        this.contact = contact;
        //
    }

    public void sendMessage(String message) {
        MsgUtil.sendMsg(getContact(), message);
        //安全发送
    }

    public void sendMessage(Message message) {
        MsgUtil.sendMsg(getContact(), message);
        //安全发送
    }

    public boolean hasPerm(String perm) {
        return hasPerm(perm, null);
        //
    }

    public boolean hasPerm(String perm, MemberPermission adminPass) {
        perm = perm.toLowerCase();
        return PermManager.hasPerm(getContactAsUser(), perm, adminPass);
    }

    public long getId() {
        return getContact().getId();
        //
    }

    public boolean isGroup() {
        return getContact() instanceof Group;
        //
    }

    public Group getContactAsGroup() {
        if (isGroup()) {
            return (Group) this.getContact();
        }
        throw new ClassCastException("其中的对象不是Group");
    }

    public User getContactAsUser() {
        if (getContact() instanceof User) {
            return (User) getContact();
        }
        throw new ClassCastException("其中的对象不是User");
    }

    public Member getContactAsMember() {
        //
        if (getContact() instanceof Member) {
            return (Member) getContact();
        }
        throw new ClassCastException("其中的对象不是Member");
    }
}
