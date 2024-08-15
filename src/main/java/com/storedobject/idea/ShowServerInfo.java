package com.storedobject.idea;

public class ShowServerInfo extends Action {

    @Override
    public void doAction() {
        message(Service.get().getServerInfo());
    }
}
