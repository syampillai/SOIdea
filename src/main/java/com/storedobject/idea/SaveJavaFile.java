package com.storedobject.idea;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

public class SaveJavaFile extends Action {

    @Override
    public void doAction() {
        if (editor == null) {
            notify("Open the file to be saved to the Server in the editor first", "Warning", NotificationType.WARNING);
            return;
        }
        if (Service.noConnection()) {
            return;
        }
        String fileName = FileUtility.getFileName(Objects.requireNonNull(FileDocumentManager.getInstance().getFile(editor.getDocument())));
        if(!fileName.endsWith(".java")) {
            message("This is not a Java file");
            return;
        }
        try {
            Reader error = Service.get().send(fileName, editor.getDocument().getText());
            if (error == null) {
                notify("Saved " + fileName + " to " + Service.get().getHost());
                return;
            }
            VirtualFile errorFile = FileUtility.getFile("error.txt");
            FileUtility.writeFile(errorFile, error);
            FileEditorManager.getInstance(Service.getProject()).openFile(errorFile, true);
            notify("Please act on the messages received from the Server!", "Error", NotificationType.ERROR);
        } catch (IOException e) {
            notify(e.getMessage(), "Error while saving file " + fileName, NotificationType.ERROR);
            message("Error saving file " + fileName);
        }
    }
}