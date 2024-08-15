package com.storedobject.idea;

public class DisconnectServer extends Action {

    @Override
    public void doAction() {
        Service.get().disconnect();
    }
}
