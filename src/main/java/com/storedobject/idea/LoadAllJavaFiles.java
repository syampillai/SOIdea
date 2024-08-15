package com.storedobject.idea;


import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;

import java.util.ArrayList;

public class LoadAllJavaFiles extends Action {

    @Override
    public void doAction() {
        if(Service.noConnection()) {
            return;
        }
        if(!confirm("All files will be loaded from the Server " + Service.get().getHost() +
                " and files in Project " + Service.getProject().getName() +
                " will be overwritten by it!")) {
            return;
        }
        ArrayList<String> files;
        try {
            files = Service.get().list("");
        } catch (Exception e) {
            message("Unable to get the list of files from the Server");
            return;
        }
        new Thread(new FileLoader(files)).start();
    }

    private static class FileLoader implements Runnable {

        private final ArrayList<String> files;

        private FileLoader(ArrayList<String> files) {
            this.files = files;
        }

        @Override
        public void run() {
            Notification n = null;
            int i, count = 0;
            for(i = 0; i < files.size(); i++) {
                if(FileUtility.loadJavaFile(files.get(i))) {
                    ++count;
                }
                if(i % 10 == 0) {
                    if (n != null) {
                        n.expire();
                    }
                    n = notify(count);
                }
            }
            notify(count);
        }

        private Notification notify(int count) {
            Notification n = new Notification("SO", "Files loaded", count + "/" + files.size(), NotificationType.INFORMATION);
            Notifications.Bus.notify(n);
            return n;
        }
    }
}
