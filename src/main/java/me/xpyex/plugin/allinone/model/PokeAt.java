package me.xpyex.plugin.allinone.model;

import java.io.File;
import me.xpyex.plugin.allinone.core.Model;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.NudgeEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

@SuppressWarnings("unused")
public class PokeAt extends Model {

    @Override
    public void register() {
        listenEvent(NudgeEvent.class, (event) -> {
            if (event.getTarget().getId() != getBot().getId()) {
                return;
            }
            MessageChain msg = new PlainText("检测到未知的外部撞击").plus("");
            Image image = Contact.uploadImage(event.component3(), new File("pictures/轻轻唤醒沉睡的心灵.png"));
            msg.plus(image);
            event.component3().sendMessage(msg);
        });
    }

    @Override
    public String getName() {
        return "PokeAt";
        //
    }
}
