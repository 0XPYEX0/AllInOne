package me.xpyex.plugin.allinone.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.jetbrains.annotations.NotNull;

public class ReflectUtil {
    @NotNull
    private static final File PLUGIN_FOLDER = new File("plugins");

    public static List<Class<?>> getClasses(String packagePath) {
        List<Class<?>> classList = new ArrayList<>();
        for (File file : PLUGIN_FOLDER.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(file)) {
                    Enumeration<JarEntry> enumFiles = jar.entries();
                    while (enumFiles.hasMoreElements()) {
                        JarEntry entry = enumFiles.nextElement();
                        if (!entry.getName().contains("META-INF")) {
                            String classPath = entry.getName().replace("/", ".");
                            if (classPath.endsWith(".class")) {
                                String className = classPath.substring(0, classPath.length() - 6);
                                if (className.contains(packagePath)) {
                                    classList.add(Class.forName(className));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    ExceptionUtil.handleException(e, false);
                }
            }
        }
        return classList;
    }
}
