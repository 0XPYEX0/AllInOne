package me.xpyex.plugin.allinone.core.command.argument;

import java.util.Optional;

public class StrParser extends ArgParser {
    @Override
    public Optional<?> parse(String arg) {
        return Optional.ofNullable(arg);
        //
    }
}
