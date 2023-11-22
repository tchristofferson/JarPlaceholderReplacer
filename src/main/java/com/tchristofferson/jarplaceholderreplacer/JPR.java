package com.tchristofferson.jarplaceholderreplacer;

import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JPR {

    public static ByteArrayOutputStream modifyJar(String inputJarPath, String loggedInUsername) throws IOException {
        JarFile jarFile = new JarFile(inputJarPath);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JarOutputStream jarOutputStream = new JarOutputStream(outputStream);

        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            // Skip entries that are not class files
            if (!entry.getName().endsWith(".class")) {
                copyEntry(jarFile, jarOutputStream, entry);
                continue;
            }

            try (InputStream entryInputStream = jarFile.getInputStream(entry)) {
                byte[] modifiedClassBytes = modifyClass(entryInputStream, loggedInUsername);
                saveModifiedClass(jarOutputStream, entry, modifiedClassBytes);
            }
        }

        return outputStream;
    }

    private static byte[] modifyClass(InputStream classInputStream, String loggedInUsername) throws IOException {
        ClassReader classReader = new ClassReader(classInputStream);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

        classReader.accept(new ClassVisitor(Opcodes.ASM7, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM7, methodVisitor) {
                    @Override
                    public void visitLdcInsn(Object cst) {
                        if (cst instanceof String) {
                            String modifiedString = ((String) cst).replace("%__USER__%", loggedInUsername);
                            super.visitLdcInsn(modifiedString);
                            return;
                        }
                        super.visitLdcInsn(cst);
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);

        return classWriter.toByteArray();
    }

    private static void copyEntry(JarFile jarFile, JarOutputStream jarOutputStream, JarEntry entry) throws IOException {
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            JarEntry newEntry = new JarEntry(entry.getName());
            jarOutputStream.putNextEntry(newEntry);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                jarOutputStream.write(buffer, 0, bytesRead);
            }
            jarOutputStream.closeEntry();
        }
    }

    private static void saveModifiedClass(JarOutputStream jarOutputStream, JarEntry entry, byte[] modifiedClassBytes) throws IOException {
        JarEntry newEntry = new JarEntry(entry.getName());
        jarOutputStream.putNextEntry(newEntry);
        jarOutputStream.write(modifiedClassBytes);
        jarOutputStream.closeEntry();
    }

}
