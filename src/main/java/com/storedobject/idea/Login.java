package com.storedobject.idea;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

class Login {

    private static Login login;
    private static boolean loggedIn = false;
    private static final ArrayList<String> servers = new ArrayList<>();
    static {
        try {
            loadHostNames();
        } catch (IOException ignored) {
        }
    }

    private Login() {
    }

    static Login get() {
        if(login == null) {
            login = new Login();
        }
        return login;
    }

    void logOff() {
        loggedIn = false;
    }

    boolean login() {
        if(loggedIn) {
            return true;
        }
        LoginDialog d = new LoginDialog(servers.toArray(new String[0]), Service.get().getLogin(), Service.get().getPassword());
        d.show();
        if(!d.isOK()) {
            return false;
        }
        String user = d.userField.getText().trim();
        String server = ((String) Objects.requireNonNull(d.serverField.getSelectedItem())).trim();
        boolean save = false;
        if(servers.isEmpty()) {
            if(!server.isEmpty()) {
                servers.add(server);
                save = true;
            }
        } else {
            if(!servers.get(0).equals(server)) {
                save = true;
                servers.remove(server);
                servers.add(0, server);
            }
        }
        if(save) {
            try {
                saveHostNames();
            } catch (Throwable ignored) {
            }
        }
        Service.get().setHost(server, user, new String(d.passwordField.getPassword()));
        loggedIn = !user.isEmpty() && !server.isEmpty();
        return loggedIn;
    }

    private static void saveHostNames() throws IOException {
        if(servers.isEmpty()) {
            return;
        }
        BufferedWriter out = new BufferedWriter(new FileWriter(System.getProperty("user.home") + File.separatorChar + ".so"));
        int i = 0;
        for(String s: servers) {
            if(s == null) {
                continue;
            }
            out.write(s);
            out.newLine();
            ++i;
            if(i >= 10) {
                break;
            }
        }
        out.close();
    }

    private static void loadHostNames() throws IOException {
        servers.clear();
        BufferedReader in = new BufferedReader(new FileReader(System.getProperty("user.home") + File.separatorChar + ".so"));
        String s;
        while((s = in.readLine()) != null) {
            servers.add(s);
        }
        in.close();
    }

    private static class LoginDialog extends DialogWrapper {

        private final ComboBox<String> serverField;
        private final JTextField userField;
        private final JPasswordField passwordField;

        private LoginDialog(String[] servers, String user, String password) {
            super(Service.getProject());
            setTitle("Login");
            if(servers.length == 0) {
                servers = new String[] { "https://..." };
            }
            serverField = new ComboBox<>(servers);
            serverField.setEditable(true);
            userField = new JTextField();
            userField.setColumns(30);
            if(user != null) {
                userField.setText(user);
            }
            passwordField = new JPasswordField();
            passwordField.setColumns(30);
            if(password != null) {
                passwordField.setText(password);
            }
            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel p = new JPanel();
            p.setLayout(new GridLayout(0, 1));
            p.add(new JLabel("Server:"));
            p.add(serverField);
            p.add(new JLabel("Login:"));
            p.add(userField);
            p.add(new JLabel("Password:"));
            p.add(passwordField);
            return p;
        }
    }
}
