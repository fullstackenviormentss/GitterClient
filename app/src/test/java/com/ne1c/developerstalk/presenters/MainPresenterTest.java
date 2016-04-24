package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.BuildConfig;
import com.ne1c.developerstalk.MockRxSchedulersFactory;
import com.ne1c.developerstalk.dataproviders.DataManger;
import com.ne1c.developerstalk.models.data.RoomModel;
import com.ne1c.developerstalk.models.data.UserModel;
import com.ne1c.developerstalk.ui.views.MainView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import rx.Observable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class MainPresenterTest {
    @Mock
    MainView view;

    @Mock
    DataManger dataManger;

    private MainPresenter presenter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        presenter = new MainPresenter(new MockRxSchedulersFactory(), dataManger);
        presenter.bindView(view);
    }

    @Test
    public void successLoadRooms() {
        ArrayList<RoomModel> rooms = new ArrayList<>();

        when(dataManger.getRooms()).thenReturn(Observable.just(rooms));

        presenter.loadRooms();

        verify(view, times(1)).showRooms(rooms);
        verify(view, never()).showError(anyInt());
    }

    @Test
    public void failLoadRooms() {
        when(dataManger.getRooms()).thenReturn(Observable.error(new Throwable("Unable to resolve api.gitter.im")));

        presenter.loadRooms();

        verify(view, never()).showRooms(any(ArrayList.class));
    }

    @Test
    public void successLoadCachedRooms() {
        ArrayList<RoomModel> rooms = new ArrayList<>();

        when(dataManger.getDbRooms()).thenReturn(Observable.just(rooms));

        presenter.loadCachedRooms();

        verify(view, times(1)).showRooms(rooms);
        verify(view, never()).showError(anyInt());
    }

    @Test
    public void successGetProfile() {
        ArrayList<UserModel> users = mock(ArrayList.class);
        UserModel user = mock(UserModel.class);
        user.id = "";
        user.avatarUrlMedium = "";

        when(users.get(0)).thenReturn(user);

        when(dataManger.getProfile()).thenReturn(Observable.just(users));

        presenter.loadProfile();

        verify(view, times(1)).showProfile(users.get(0));
        verify(view, never()).showError(anyInt());
    }

    @Test
    public void failGetProfile() {
        when(dataManger.getProfile()).thenReturn(Observable.error(new Throwable("error")));

        presenter.loadProfile();

        verify(view, times(1)).showProfile(any(UserModel.class));
    }

    @Test
    public void leaveFromRoom() {
        ResponseBody response = any(ResponseBody.class);

        //when(dataManger.leaveFromRoom("room_id")).thenReturn(Observable.just(response));

        presenter.leaveFromRoom("room_id");

        verify(view).leavedFromRoom();
    }

    @After
    public void end() {
        presenter.unbindView();
    }
}