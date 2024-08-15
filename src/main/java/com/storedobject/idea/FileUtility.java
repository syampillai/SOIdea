package com.storedobject.idea;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

class FileUtility {

    static VirtualFile getFile(String file) throws IOException {
        return WriteCommandAction.runWriteCommandAction(Service.getProject(), getFileAction(getRoot(file), file));
    }

    private static ThrowableComputable<VirtualFile, IOException> getFileAction(VirtualFile root, String file) {
        return () -> getFileInt(root, file);
    }

    private static VirtualFile getRoot(String file) {
        return file.endsWith(".java") ? Action.javaFolder : Action.resourceFolder;
    }

    private static VirtualFile getFileInt(VirtualFile root, String file) throws IOException {
        int p;
        String dirName;
        VirtualFile dir;
        while ((p = file.indexOf('/')) >= 0) {
            if(p == 0) {
                file = file.substring(1);
                continue;
            }
            dirName = file.substring(0, p);
            dir = root.findChild(dirName);
            if(dir == null) {

                dir = root.createChildDirectory(null, dirName);
            } else {
                if(!dir.isDirectory()) {
                    return null;
                }
            }
            root = dir;
            if(p == file.length() - 1) {
                return root;
            }
            file = file.substring(p + 1);
        }
        if(file.contains(".")) {
            return root.findOrCreateChildData(null, file);
        }
        return root.createChildDirectory(null, file);
    }

    private static void writeFile(VirtualFile file, InputStream data) throws IOException {
        writeFile(file, new InputStreamReader(data, StandardCharsets.UTF_8));
    }

    static void writeFile(VirtualFile file, Reader data) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(file.getOutputStream(null)));
        BufferedReader r = new BufferedReader(data);
        WriteCommandAction.runWriteCommandAction(Service.getProject(), copyAction(r, w));
        try {
            r.close();
        } catch (IOException ignored) {
        }
    }

    private static ThrowableComputable<Boolean, IOException> copyAction(BufferedReader r, BufferedWriter w) {
        return () -> copy(r, w);
    }

    private static boolean copy(BufferedReader r, BufferedWriter w) throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            w.write(line);
            w.write("\n");
        }
        w.close();
        return true;
    }

    static String getFileName(VirtualFile file) {
        String url = file.getUrl();
        String dir = Action.javaFolder.getUrl();
        if(url.startsWith(dir)) {
            return url.substring(dir.length() + 1).replace(File.separatorChar, '.');
        }
        dir = Action.resourceFolder.getUrl();
        if(url.startsWith(dir)) {
            url = url.substring(dir.length() + 1);
        }
        return url.replace('/', File.separatorChar);
    }

    static boolean loadJavaFile(String fileName) {
        VirtualFile file = null;
        try {
            file = getFile(fileName.replace(".", File.separator) + ".java");
            FileUtility.writeFile(file, Service.get().load(fileName));
            return true;
        } catch (Exception e) {
            Action.notify("File " +
                    (file == null ? fileName : file.getCanonicalPath()), "Unable to create", NotificationType.ERROR);
        }
        return false;
    }
}
