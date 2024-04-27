package me.xpyex.plugin.allinone.core.command.argument;

import net.mamoe.mirai.contact.Contact;

public abstract class ContactParser extends ArgParser {
    public long getParsedId(String arg) {
        return parse(arg, Contact.class).isPresent() ? parse(arg, Contact.class).get().getId() : 0;
    }
}
