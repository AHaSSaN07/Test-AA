package io.github.ahassan07.carvideo;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Template;

/** Car keyboard screen. Android Auto only offers the keyboard while parked. */
final class SearchScreen extends Screen {

    SearchScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        SearchTemplate.SearchCallback callback = new SearchTemplate.SearchCallback() {
            @Override
            public void onSearchSubmitted(@NonNull String searchText) {
                String url = Prefs.fromInput(searchText);
                if (url != null) {
                    setResult(url);
                }
                finish();
            }
        };
        return new SearchTemplate.Builder(callback)
                .setHeaderAction(Action.BACK)
                .setSearchHint(getCarContext().getString(R.string.car_search_hint))
                .setShowKeyboardByDefault(true)
                .build();
    }
}
