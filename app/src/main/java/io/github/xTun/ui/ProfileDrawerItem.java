package io.github.xTun.ui;

import com.mikepenz.materialdrawer.model.AbstractBadgeableDrawerItem;

class ProfileDrawerItem extends AbstractBadgeableDrawerItem<ProfileDrawerItem> {
    private int id;

    ProfileDrawerItem withProfileId(int id) {
        this.id = id;
        return this;
    }

    int getProfileId() {
        return id;
    }
}

