package com.ne1c.developerstalk.di.modules;

import android.content.Context;

import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.di.annotations.PerApplication;
import com.ne1c.developerstalk.services.DataManger;
import com.ne1c.developerstalk.utils.Utils;

import dagger.Module;
import dagger.Provides;
import retrofit.RestAdapter;

@Module(includes = DatabaseModule.class)
public class ApplicationModule {
    private Application mApp;

    public ApplicationModule(Application mApp) {
        this.mApp = mApp;
    }

    @PerApplication
    @Provides
    public DataManger provideDataManager() {
        return new DataManger(mApp);
    }

    @PerApplication
    @Provides
    public GitterApi provideApi() {
        return new RestAdapter.Builder()
                .setEndpoint(Utils.GITTER_API_URL)
                .build()
                .create(GitterApi.class);
    }

    @PerApplication
    @Provides
    public Context provideContext() {
        return mApp;
    }
}