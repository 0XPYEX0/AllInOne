package me.xpyex.plugin.allinone.core.permission;

import java.util.TreeSet;

public interface Perms {
    static TreeSet<String> getLowerCaseSet(TreeSet<String> set) {
        TreeSet<String> newOne = new TreeSet<>();
        for (String s : set) {
            newOne.add(s.toLowerCase());
        }
        return newOne;
    }

    TreeSet<String> getPermissions();

    TreeSet<String> getDenyPerms();

    void save();
}
