package com.storedobject.idea;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Objects;

public class LoadJavaFile extends Action {

    @Override
    public void doAction() {
        String currentFileName = null;
        if(editor != null) {
            try {
                currentFileName = FileUtility.getFileName(Objects.requireNonNull(FileDocumentManager.getInstance().getFile(editor.getDocument())));
            } catch(Throwable ignored) {
            }
        }
        if(Service.noConnection()) {
            return;
        }
        do {
            SelectFile selectFile = new SelectFile(currentFileName);
            selectFile.show();
            if (!selectFile.isOK()) {
                return;
            }
            currentFileName = selectFile.fileNameField.getText().trim();
        } while (currentFileName.isEmpty());
        ArrayList<String> fileNames;
        try {
            fileNames = Service.get().list(currentFileName);
        } catch (Exception e) {
            message("Error retrieving file names!");
            return;
        }
        if(fileNames.isEmpty()) {
            message("No files matching '" + currentFileName + "' found!");
            return;
        }
        if(fileNames.size() == 1) {
            currentFileName = fileNames.get(0);
        } else {
            ChooseFile chooseFile = new ChooseFile(fileNames);
            if(fileNames.contains(currentFileName)) {
                chooseFile.setCurrentFile(currentFileName);
            }
            chooseFile.show();
            if (!chooseFile.isOK()) {
                return;
            }
            currentFileName = (String) chooseFile.filesField.getSelectedItem();
        }
        if(FileUtility.loadJavaFile(currentFileName)) {
            notify("Loaded " + currentFileName + ".java from " + Service.get().getHost());
        }
    }

    private static class SelectFile extends DialogWrapper {

        private final JTextField host;
        private final JTextField fileNameField;

        private SelectFile(String fileName) {
            super(Service.getProject());
            setTitle("Enter Full / Part File Name");
            host = new JTextField(Service.get().getHost());
            host.setEditable(false);
            fileNameField = new JTextField();
            fileNameField.setColumns(60);
            if(fileName != null) {
                fileNameField.setText(fileName);
            }
            fileNameField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    fileNameField.select(0, fileNameField.getText().length());
                }

                @Override
                public void focusLost(FocusEvent e) {
                }
            });
            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel p = new JPanel();
            p.setLayout(new GridLayout(0, 1));
            p.add(new JLabel("Server:"));
            p.add(host);
            p.add(new JLabel("File Name:"));
            p.add(fileNameField);
            return p;
        }
    }

    private static class ChooseFile extends DialogWrapper {

        private final ComboBox<String> filesField;

        private ChooseFile(ArrayList<String> files) {
            super(Service.getProject());
            setTitle("Choose File");
            filesField = new ComboBox<>(files.toArray(new String[0]));
            filesField.setMinimumAndPreferredWidth(800);
            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel p = new JPanel();
            p.setLayout(new GridLayout(0, 1));
            p.add(new JLabel("File:"));
            p.add(filesField);
            return p;
        }

        private void setCurrentFile(String currentFile) {
            filesField.setSelectedItem(currentFile);
        }
    }
}
