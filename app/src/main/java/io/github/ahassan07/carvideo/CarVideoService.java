package io.github.ahassan07.carvideo;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;

/** Entry point Android Auto binds to. */
public final class CarVideoService extends CarAppService {

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        // A sideloaded personal app: any Android Auto host may connect.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull
    @Override
    public Session onCreateSession() {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                return new BrowserScreen(getCarContext());
            }
        };
    }
}
