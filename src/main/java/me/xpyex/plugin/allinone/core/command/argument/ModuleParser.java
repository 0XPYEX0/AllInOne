package me.xpyex.plugin.allinone.core.command.argument;

import java.util.Optional;
import me.xpyex.plugin.allinone.core.module.Module;

public class ModuleParser extends ArgParser {
    @Override
    public Optional<Module> parse(String arg) {
        return Optional.ofNullable(Module.getModule(arg));
        //
    }
}
