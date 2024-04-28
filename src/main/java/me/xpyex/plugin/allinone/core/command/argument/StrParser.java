package me.xpyex.plugin.allinone.core.command.argument;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class StrParser extends ArgParser {
    @NotNull
    @Override
    public Optional<?> parse(String arg) {
        return Optional.ofNullable(arg);
        //
    }
}
