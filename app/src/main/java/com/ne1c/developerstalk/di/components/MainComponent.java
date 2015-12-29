package com.ne1c.developerstalk.di.components;

import com.ne1c.developerstalk.di.HasPresenter;
import com.ne1c.developerstalk.di.annotations.PerActivity;
import com.ne1c.developerstalk.di.modules.MainPresenterModule;
import com.ne1c.developerstalk.presenters.MainPresenter;
import com.ne1c.developerstalk.ui.activities.MainActivity;

import dagger.Component;

@PerActivity
@Component(modules = MainPresenterModule.class, dependencies = ApplicationComponent.class)
public interface MainComponent extends HasPresenter<MainPresenter> {
    void inject(MainActivity activity);
}