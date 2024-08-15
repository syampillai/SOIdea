package com.storedobject.idea;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

abstract class Action extends AnAction {

    static Editor editor;
    static VirtualFile javaFolder, resourceFolder;

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (Service.changeProject(e.getProject()) || javaFolder == null) {
            Project project = Service.getProject();
            if(project != null) {
                VirtualFile[] vFiles = ProjectRootManager.getInstance(project).getContentSourceRoots();
                for (VirtualFile f : vFiles) {
                    if (f.getUrl().endsWith("/src/main/java")) {
                        javaFolder = f;
                    } else if (f.getUrl().endsWith("/src/main/resources")) {
                        resourceFolder = f;
                    }
                }
                if(javaFolder == null || resourceFolder == null) {
                    StringBuilder m = new StringBuilder("Please create -");
                    if(javaFolder == null) {
                        m.append("\nFolder: src/main/java");
                    }
                    if(resourceFolder == null) {
                        m.append("\nFolder: src/main/resources");
                    }
                    message(m);
                }
            }
        }
        editor = e.getData(CommonDataKeys.EDITOR);
        doAction();
    }

    static String getJavaFolderName() {
        if (javaFolder == null) {
            return "<Not set>";
        }
        return javaFolder.getPath();
    }

    static String getResourceFolderName() {
        if (resourceFolder == null) {
            return "<Not set>";
        }
        return resourceFolder.getPath();
    }

    protected abstract void doAction();

    static void message(Object message) {
        Messages.showMessageDialog(Service.getProject(), message.toString(), "Message", Messages.getInformationIcon());
    }

    static boolean confirm(String message) {
        Confirm confirm = new Confirm(message);
        confirm.show();
        return confirm.isOK();
    }

    private static class Confirm extends DialogWrapper {

        private final JLabel message;

        private Confirm(String message) {
            super(Service.getProject());
            setTitle("Confirm");
            this.message = new JLabel(message);
            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return message;
        }
    }

    static void notify(String message) {
        notify(message, "Message", NotificationType.INFORMATION);
    }

    static void notify(String message, String title, NotificationType type) {
        Notification n = new Notification("SO", title, message, type);
        Notifications.Bus.notify(n);
    }
}