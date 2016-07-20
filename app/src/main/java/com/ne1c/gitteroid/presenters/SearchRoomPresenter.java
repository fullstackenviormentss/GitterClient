package com.ne1c.gitteroid.presenters;

import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.models.RoomMapper;
import com.ne1c.gitteroid.models.data.RoomModel;
import com.ne1c.gitteroid.models.view.RoomViewModel;
import com.ne1c.gitteroid.ui.views.RoomsListView;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

public class SearchRoomPresenter extends BasePresenter<RoomsListView> {
    // All rooms for edit
    // Set this list to adapter if user will edit
    private ArrayList<RoomViewModel> mAllRooms = new ArrayList<>();

    private RoomsListView mView;

    private DataManger mDataManger;
    private RxSchedulersFactory mSchedulersFactory;

    private CompositeSubscription mSubscriptions;
    private PublishSubject<String> mSearchRoomSubject = PublishSubject.create();

    @Inject
    public SearchRoomPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        mSchedulersFactory = factory;
        mDataManger = dataManger;
    }

    @Override
    public void bindView(RoomsListView view) {
        mView = view;
        mSubscriptions = new CompositeSubscription();

        Subscription sub = mSearchRoomSubject
                .asObservable()
                .throttleLast(1, TimeUnit.SECONDS)
                .flatMap(query -> mDataManger.searchRooms(query))
                .map(response -> {
                    // Show without exist in db
                    ArrayList<RoomModel> responseRooms = response.getResult();
                    ArrayList<RoomViewModel> filterRooms = new ArrayList<>();

                    boolean existInDb;

                    for (RoomModel responseRoom : responseRooms) {
                        existInDb = false;

                        for (RoomViewModel dbRoom : mAllRooms) {
                            if (responseRoom.id.equals(dbRoom.id)) {
                                existInDb = true;
                                break;
                            }
                        }

                        if (!existInDb) {
                            filterRooms.add(RoomMapper.mapToView(responseRoom));
                        }
                    }

                    return filterRooms;
                })
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(response -> {
                    mView.dismissDialog();
                    mView.resultSearch(response);
                }, throwable -> {
                    mView.errorSearch();
                });

        mSubscriptions.add(sub);
    }

    @Override
    public void unbindView() {
        mSubscriptions.unsubscribe();

        mView = null;
    }

    @Override
    public void onDestroy() {

    }

    public List<RoomViewModel> getAllRooms() {
        return mAllRooms;
    }

    public void searchRooms(String query) {
        if (!query.isEmpty()) {
            mView.showDialog();
            mSearchRoomSubject.onNext(query);
        } else {
            mView.resultSearch(new ArrayList<>());
            mView.dismissDialog();
        }
    }
}