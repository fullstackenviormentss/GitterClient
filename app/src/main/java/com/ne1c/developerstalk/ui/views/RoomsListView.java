package com.ne1c.developerstalk.ui.views;

import com.ne1c.developerstalk.models.RoomModel;

import java.util.List;

public interface RoomsListView extends BaseView {
    void showRooms(List<RoomModel> rooms);

    void showError(String text);

    void showDialog();

    void dismissDialog();
}