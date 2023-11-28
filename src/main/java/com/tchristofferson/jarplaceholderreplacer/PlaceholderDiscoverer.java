package com.tchristofferson.jarplaceholderreplacer;

import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PlaceholderDiscoverer {

    /**
     * @see PlaceholderDiscoverer#discover(JarFile, Collection)
     */
    public static List<PlaceholderLocationInfo> discover(JarFile jarFile, String... placeholders) {
        return discover(jarFile, Arrays.asList(placeholders));
    }

    /**
     * Will find specified placeholders in class files in the specified jar file
     * @param jarFile The jar file containing classes containing placeholders to be found
     * @param placeholders The placeholders to look for in the jar file
     * @return A {@link List<PlaceholderLocationInfo>} representing classes with placeholders
     */
    public static List<PlaceholderLocationInfo> discover(JarFile jarFile, Collection<String> placeholders) {
        //key=class
        Map<String, PlaceholderLocationInfo> discoveredPlaceholders = new HashMap<>();
        Enumeration<JarEntry> entries = jarFile.entries();

        //Loop over each file in jar
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            //Only works with class files, so if it isn't a class file skip
            if (!entry.getName().endsWith(".class"))
                continue;

            ClassReader classReader;

            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                classReader = new ClassReader(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to get input stream for jar file!", e);
            }

            //Iterator for copy of placeholders. If a placeholder is found in this class, it will be removed from this iterator
            //  after the info is added to the discovered placeholders. This is to make searching faster,
            //  no need to search for a placeholder already found in the class again.
            //Use set for uniqueness
            Iterator<String> searchablePlaceholdersIterator = new HashSet<>(placeholders).iterator();
            classReader.accept(new ClassVisitor(Opcodes.ASM9, new ClassWriter(ClassWriter.COMPUTE_FRAMES)) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, methodVisitor) {
                        @Override
                        public void visitLdcInsn(Object cst) {
                            //If variable is a String check for placeholder
                            if (cst instanceof String) {
                                while (searchablePlaceholdersIterator.hasNext()) {
                                    String placeholder = searchablePlaceholdersIterator.next();

                                    //If placeholder is found in String
                                    if (((String) cst).contains(placeholder)) {
                                        discoveredPlaceholders
                                            .computeIfAbsent(entry.getName(), clazz -> new PlaceholderLocationInfo(entry.getName()))
                                            .addPlaceholder(placeholder);

                                        //Remove to increase search speed. Once found in class remove form iterator
                                        searchablePlaceholdersIterator.remove();
                                        //Once the placeholder is found no need to keep trying to see if other placeholders apply to this variable
                                        break;
                                    }
                                }
                            }

                            super.visitLdcInsn(cst);
                        }
                    };
                }
            }, ClassReader.SKIP_FRAMES);
        }

        return new ArrayList<>(discoveredPlaceholders.values());
    }
}
