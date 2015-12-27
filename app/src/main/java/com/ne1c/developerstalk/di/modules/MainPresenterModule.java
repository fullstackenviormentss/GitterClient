package com.ne1c.developerstalk.di.modules;


import com.ne1c.developerstalk.di.annotations.PerActivity;
import com.ne1c.developerstalk.presenters.MainPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class MainPresenterModule {
    @PerActivity
    @Provides
    public MainPresenter provideMainPresenter() {
        return new MainPresenter();
    }
}