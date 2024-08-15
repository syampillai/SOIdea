package com.storedobject.idea;

public class SwitchServer extends Action {

    @Override
    public void doAction() {
        Service.get().switchServer();
        notify("Server switched");
    }
}
