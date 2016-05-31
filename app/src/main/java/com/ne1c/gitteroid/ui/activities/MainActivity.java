package com.ne1c.gitteroid.ui.activities;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.ne1c.gitteroid.Application;
import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.di.components.DaggerMainComponent;
import com.ne1c.gitteroid.di.components.MainComponent;
import com.ne1c.gitteroid.di.modules.MainPresenterModule;
import com.ne1c.gitteroid.events.NewMessageEvent;
import com.ne1c.gitteroid.events.ReadMessagesEvent;
import com.ne1c.gitteroid.events.RefreshMessagesRoomEvent;
import com.ne1c.gitteroid.models.MessageMapper;
import com.ne1c.gitteroid.models.RoomMapper;
import com.ne1c.gitteroid.models.data.MessageModel;
import com.ne1c.gitteroid.models.data.RoomModel;
import com.ne1c.gitteroid.models.data.UserModel;
import com.ne1c.gitteroid.models.view.RoomViewModel;
import com.ne1c.gitteroid.presenters.MainPresenter;
import com.ne1c.gitteroid.services.NotificationService;
import com.ne1c.gitteroid.ui.adapters.RoomsPagerAdapter;
import com.ne1c.gitteroid.ui.views.MainView;
import com.ne1c.gitteroid.utils.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MainActivity extends BaseActivity implements MainView {
    public final static String BROADCAST_UNAUTHORIZED = "com.ne1c.gitterclient.UnathorizedReceiver";
    public final static String BROADCAST_NEW_MESSAGE = "com.ne1c.gitterclient.NewMessageReceiver";
    public final static String MESSAGE_INTENT_KEY = "message";
    public final static String ROOM_ID_INTENT_KEY = "room";

    public final int START_ROOM_IN_DRAWER_OFFSET = 2;
    private final String SELECT_NAV_ITEM_BUNDLE = "select_nav_item";
    private final String ROOMS_BUNDLE = "rooms_bundle";
    private final String ROOMS_IN_DRAWER_BUNDLE = "rooms_in_drawer_bundle";
    private final String ROOMS_IN_TABS_BUNDLE = "rooms_in_tabs_bundle";

    // private ChatRoomFragment mChatRoomFragment;
    private ViewPager mRoomsViewPager;
   // private RoomsPagerAdapter mRoomsPagerAdapter;

    private ActionBarDrawerToggle mDrawerToggle;

    private TabLayout mRoomTabs;

    private Drawer mDrawer;
    private ArrayList<IDrawerItem> mDrawerItems = new ArrayList<>();
    private AccountHeader mAccountHeader;
    private IProfile mMainProfile = new ProfileDrawerItem();

    private ArrayList<RoomViewModel> mRoomsList = new ArrayList<>();
    private ArrayList<RoomViewModel> mRoomsInDrawer = new ArrayList<>(); // Rooms that user will see
    private ArrayList<RoomViewModel> mRoomsInTabs = new ArrayList<>();

    private MainComponent mComponent;

    @Inject
    MainPresenter mPresenter;

    private Drawer.OnDrawerItemClickListener mDrawerItemClickListener = new Drawer.OnDrawerItemClickListener() {
        @Override
        public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
            final PrimaryDrawerItem item = (PrimaryDrawerItem) drawerItem;

            final String name = item.getName().getText();

            if (name.equals(getString(R.string.home))) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gitter.im/home")));
            } else if (name.equals(getString(R.string.action_settings))) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            } else if (name.equals(getString(R.string.signout))) {
                signOut();
            } else if (name.equals(getString(R.string.all))) {
                showRoomsListDialog();
            } else if (mRoomsInDrawer.size() > 0) {
                clickOnRoomInDrawer(name);
            }

            mDrawer.closeDrawer();

            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.getInstance().getAccessToken().isEmpty()) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        mPresenter.onCreate();
        mPresenter.bindView(this);

        EventBus.getDefault().register(this);
        registerReceiver(newMessageReceiver, new IntentFilter(BROADCAST_NEW_MESSAGE));

        initViews();

        initState(savedInstanceState);

        initDrawerImageLoader();
    }

    @Override
    protected void initDiComponent() {
        mComponent = DaggerMainComponent.builder()
                .applicationComponent(((Application) getApplication()).getComponent())
                .mainPresenterModule(new MainPresenterModule())
                .build();

        mComponent.inject(this);
    }

    private void initState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            initWithSavedInstanceState(savedInstanceState);
        } else {
            initWithoutSavedInstanceState();
        }
    }

    private void initDrawerImageLoader() {
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Glide.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Glide.clear(imageView);
            }
        });
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        mRoomsViewPager = (ViewPager) findViewById(R.id.rooms_viewPager);
        //mRoomsPagerAdapter = new RoomsPagerAdapter(getSupportFragmentManager(), mRoomsInTabs);
        mRoomsViewPager.setAdapter(new RoomsPagerAdapter(getSupportFragmentManager(), mRoomsInTabs));

        mRoomTabs = (TabLayout) findViewById(R.id.rooms_tab);
        mRoomTabs.setupWithViewPager(mRoomsViewPager);

        setNavigationView();
    }

    private void initWithSavedInstanceState(@NotNull Bundle savedInstanceState) {
        final int selectedNavItem = savedInstanceState.getInt(SELECT_NAV_ITEM_BUNDLE);
        final ArrayList<RoomViewModel> roomsList = savedInstanceState.getParcelableArrayList(ROOMS_BUNDLE);

        addRoomsToDrawer(roomsList, true);

        if (mRoomsList.size() > 0) {
            // If activity open from notification
            // mActiveRoom = getIntent().getParcelableExtra(NotificationService.FROM_ROOM_EXTRA_KEY);
//
//            if (mActiveRoom == null || mActiveRoom.id == null) {
//                mActiveRoom = mRoomsList.get(mDrawer.getCurrentSelectedPosition() - 1);
//            }

            mDrawer.setSelectionAtPosition(selectedNavItem + 1);

            setTitle(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(selectedNavItem)).getName().toString());
        }
    }

    private void initWithoutSavedInstanceState() {
        mPresenter.loadRooms(false);

        if (Utils.getInstance().isNetworkConnected()) {
            mPresenter.loadRooms(true);
        }

        mPresenter.loadProfile();
    }

    private void addRoomsToDrawer(ArrayList<RoomViewModel> roomsList, boolean restore) {
        mRoomsList.clear();
        mRoomsList.addAll(roomsList);

        if (!restore) {
            mRoomsInDrawer.clear();
        }

        for (RoomViewModel room : roomsList) {
            if (room.unreadItems > 0) {
                mDrawer.addItemAtPosition(formatRoomToDrawerItem(room),
                        mDrawer.getDrawerItems().size() - START_ROOM_IN_DRAWER_OFFSET);

                if (!restore) {
                    mRoomsInDrawer.add(room);
                }
            }
        }

        if (mRoomsInDrawer.size() == 0 && mRoomsList.size() > 0 && !restore) {
            final int endValue = mRoomsList.size() >= 4 ? 4 : mRoomsInDrawer.size();
            mRoomsInDrawer.addAll(mRoomsList.subList(0, endValue));
        }

        mDrawer.getAdapter().notifyDataSetChanged();
    }

    private PrimaryDrawerItem formatRoomToDrawerItem(RoomViewModel room) {
        BadgeStyle badgeStyle = new BadgeStyle(getResources().getColor(R.color.md_green_500),
                getResources().getColor(R.color.md_green_700));
        badgeStyle.withTextColor(getResources().getColor(android.R.color.white));

        String badgeText = room.unreadItems == 100 ? "99+" : Integer.toString(room.unreadItems);
        return new PrimaryDrawerItem()
                .withIcon(R.drawable.ic_room)
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withName(room.name)
                .withTextColor(Color.WHITE)
                .withSelectedTextColor(Color.BLACK)
                .withSelectedColorRes(R.color.md_white_1000)
                .withBadge(badgeText)
                .withBadgeStyle(badgeStyle)
                .withSelectable(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(unauthorizedReceiver, new IntentFilter(BROADCAST_UNAUTHORIZED));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // If get intent from notification
        if (mRoomsList != null) {
            RoomModel intentRoom = intent.getParcelableExtra(NotificationService.FROM_ROOM_EXTRA_KEY);
            RoomViewModel roomViewModel = RoomMapper.mapToView(intentRoom);
            // If selected room not equal room id from notification, than load room
//            if (mActiveRoom == null || !mActiveRoom.id.equals(intentRoom.id)) {
//                mActiveRoom = roomViewModel;
//
//                int selectedNavItem = 0;
//                for (int i = 0; i < mRoomsList.size(); i++) {
//                    if (mRoomsList.get(i).id.equals(mActiveRoom.id)) {
//                        selectedNavItem = i + 1;
//                    }
//                }
//
//                mDrawer.setSelectionAtPosition(selectedNavItem, true);
//                if (mRoomsList.size() > 0) {
//                    EventBus.getDefault().post(mActiveRoom);
//                }
//                //  setTitle(mRoomsList.get(selectedNavItem - 1).name);
//            }

            mDrawer.closeDrawer();
        }
    }

    private void setNavigationView() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.parent_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name);

        mPresenter.loadProfile();

        createAccountHeader();

        mDrawerItems.add(new PrimaryDrawerItem()
                .withIcon(R.drawable.ic_home)
                .withName(getString(R.string.home))
                .withTextColor(Color.WHITE)
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withSelectable(false)
                .withSetSelected(false));


        mDrawerItems.add(new DividerDrawerItem());

        mDrawerItems.add(new PrimaryDrawerItem()
                .withName(getString(R.string.action_settings))
                .withIcon(R.drawable.ic_settings_dark)
                .withTextColor(Color.WHITE)
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withSelectable(false)
                .withSetSelected(false));

        mDrawerItems.add(new PrimaryDrawerItem()
                .withIcon(R.drawable.ic_logout)
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withName(getString(R.string.signout))
                .withTextColor(Color.WHITE));

        mDrawer = new DrawerBuilder().withActivity(this)
                .withTranslucentStatusBar(false)
                .withAccountHeader(mAccountHeader)
                .withActionBarDrawerToggle(mDrawerToggle)
                .withActionBarDrawerToggle(true)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(true)
                .withSliderBackgroundColorRes(R.color.navDrawerBackground)
                .addDrawerItems((IDrawerItem[]) mDrawerItems.toArray(new IDrawerItem[mDrawerItems.size()]))
                .withOnDrawerItemClickListener(mDrawerItemClickListener)
                .build();
    }

    private void createAccountHeader() {
        mAccountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withProfileImagesClickable(true)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(mMainProfile)
                .withOnAccountHeaderListener((view, iProfile, b) -> {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(Utils.GITHUB_URL + Utils.getInstance().getUserPref().url)));
                    return false;
                })
                .build();

        mAccountHeader.setActiveProfile(mMainProfile);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SELECT_NAV_ITEM_BUNDLE, mDrawer.getCurrentSelectedPosition());
        outState.putParcelableArrayList(ROOMS_BUNDLE, mRoomsList);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                if (!mDrawer.isDrawerOpen()) {
                    mDrawer.openDrawer();
                } else {
                    mDrawer.closeDrawer();
                }
                break;
            case R.id.action_refresh:
                if (getSelectedRoom() != null) {
                    EventBus.getDefault().post(new RefreshMessagesRoomEvent(getSelectedRoom()));

                    mPresenter.loadRooms(true);
                }
                break;
            case R.id.action_leave:
                if (getSelectedRoom() != null && getSelectedRoom().oneToOne) {
                    Toast.makeText(getApplicationContext(), R.string.leave_from_one_to_one, Toast.LENGTH_SHORT).show();
                    break;
                }

                mPresenter.leaveFromRoom(getSelectedRoom().id);

                break;
            case R.id.close_room:
                closeRoom();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setItemsDrawer(ArrayList<RoomViewModel> data) {
        cleanDrawer();

        addRoomsToDrawer(data, false);
        // mRoomTabs.removeAllTabs();
        // Select first room
        mDrawer.setSelectionAtPosition(START_ROOM_IN_DRAWER_OFFSET, true);
    }

    private void cleanDrawer() {
        mRoomsList.clear();
        mRoomsInDrawer.clear();
        mRoomTabs.removeAllTabs();
        mRoomsViewPager.getAdapter().notifyDataSetChanged();

        // Remove old items
        // 4 but items: "Home", "Divider", "Settings", "Sign Out".
        while (mDrawer.getDrawerItems().size() != 4) {
            mDrawer.removeItemByPosition(START_ROOM_IN_DRAWER_OFFSET); // 2? Wtf?
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            unregisterReceiver(unauthorizedReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);

        try {
            unregisterReceiver(newMessageReceiver);
        } catch (IllegalArgumentException e) {
            // Broadcast not registered
        }

        mPresenter.unbindView();
        mPresenter.onDestroy();

        mComponent = null;
        super.onDestroy();
    }

    private BroadcastReceiver unauthorizedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Select "Sign Out"
            mDrawer.setSelectionAtPosition(mDrawer.getDrawerItems().size() - 1);
        }
    };

    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (int i = 0; i < mRoomsList.size(); i++) {
                String roomId = intent.getStringExtra(ROOM_ID_INTENT_KEY);
                MessageModel message = intent.getParcelableExtra(MESSAGE_INTENT_KEY);

                if (mRoomsList.get(i).id.equals(roomId)) {
                    mRoomsList.get(i).unreadItems += 1;

                    final String badgeText = mRoomsList.get(i).unreadItems >= 100 ? "99+" : Integer.toString(mRoomsList.get(i).unreadItems);
                    mDrawer.updateItem(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(i + 1)).withBadge(badgeText));
                    mDrawer.getAdapter().notifyDataSetChanged();

                    EventBus.getDefault().post(new NewMessageEvent(MessageMapper.mapToView(message), mRoomsList.get(i)));
                }
            }
        }
    };

    // Update Badge in navigation item, if message was read
    public void onEvent(ReadMessagesEvent count) {
        for (int i = 0; i < mRoomsList.size(); i++) {
            if (mRoomsList.get(i).id.equals(getSelectedRoom().id)) {
                // Update mActiveRoom, fix it.
                // Make update will mActiveRoom while first load room or refresh room
//                mActiveRoom = mRoomsList.get(i);
//                mActiveRoom.unreadItems -= count.getCountRead();

//                if (mActiveRoom.unreadItems <= 0) {
//                    mActiveRoom.unreadItems = 0;
//                    // Remove badge
//                    mDrawer.updateItem(
//                            ((PrimaryDrawerItem) mDrawer.getDrawerItems().get(i + 1)).withBadge(new StringHolder(null)));
//                } else {
//                    String badgeText = mActiveRoom.unreadItems >= 100 ? "99+" : Integer.toString(mActiveRoom.unreadItems);
//                    mDrawer.updateItem(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(i + 1)).withBadge(badgeText));
//                }
//
//                mRoomsList.set(i, mActiveRoom);
                mDrawer.getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public void showRooms(ArrayList<RoomViewModel> rooms) {
        setItemsDrawer(rooms);
        // mRoomsPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void showProfile(UserModel user) {
        mMainProfile.withName(user.username);
        mMainProfile.withEmail(user.displayName);
        mMainProfile.withIcon(user.avatarUrlMedium);

        mAccountHeader.updateProfile(mMainProfile);
    }

    @Override
    public void showError(int resId) {
        Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void leavedFromRoom() {
        finish();
    }

    private void showRoomsListDialog() {

    }

    private void clickOnRoomInDrawer(String name) {
        if (!roomAlreadyOpened(name)) {
            for (RoomViewModel model : mRoomsInDrawer) {
                if (name.equals(model.name)) {
                    setTitle(model.name);
                    mRoomTabs.addTab(mRoomTabs.newTab(), true);
                    mRoomsInTabs.add(mRoomTabs.getSelectedTabPosition(), model);
                    mRoomsViewPager.getAdapter().notifyDataSetChanged();
                }
            }
        } else {
            mRoomsViewPager.setCurrentItem(getSelectionRoomPositionInTab(name), true);
        }
    }

    private boolean roomAlreadyOpened(String name) {
        return getSelectionRoomPositionInTab(name) != -1;
    }

    private int getSelectionRoomPositionInTab(String name) {
        for (int i = 0; i < mRoomsInTabs.size(); i++) {
            if (mRoomsInTabs.get(i).name.equals(name)) {
                return i;
            }
        }

        return -1;
    }

    private RoomViewModel getSelectedRoom() {
        final int index = mDrawer.getCurrentSelectedPosition() - START_ROOM_IN_DRAWER_OFFSET;

        if (index >= 0 && index < mRoomsInDrawer.size()) {
            return mRoomsInDrawer.get(index);
        }

        return null;
    }

    private void closeRoom() {
        if (mRoomTabs.getTabCount() > 1) {
            final int nowPos = mRoomTabs.getSelectedTabPosition();
            mRoomTabs.removeTabAt(nowPos);
            mRoomsInTabs.remove(nowPos);
            mRoomsViewPager.getAdapter().notifyDataSetChanged();
        } else if (mRoomTabs.getTabCount() == 0) {
            Toast.makeText(this, R.string.nothing_to_close, Toast.LENGTH_SHORT).show();
        } else if (mRoomTabs.getTabCount() == 1) {
            Toast.makeText(this, R.string.cannt_close_single_room, Toast.LENGTH_SHORT).show();
        }

    }

    private void signOut() {
        getSharedPreferences(Utils.getInstance().USERINFO_PREF, MODE_PRIVATE)
                .edit().clear().apply();

        startActivity(new Intent(getApplicationContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
        stopService(new Intent(getApplicationContext(), NotificationService.class));
        finish();
    }
}