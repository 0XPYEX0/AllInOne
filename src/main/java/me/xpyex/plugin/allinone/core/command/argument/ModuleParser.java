package me.xpyex.plugin.allinone.core.command.argument;

import java.util.Optional;
import me.xpyex.plugin.allinone.core.module.Module;
import org.jetbrains.annotations.NotNull;

public class ModuleParser extends ArgParser {
    @NotNull
    @Override
    public Optional<Module> parse(String arg) {
        return Optional.ofNullable(Module.getModule(arg));
        //
    }
}
