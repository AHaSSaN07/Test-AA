package io.github.ahassan07.carvideo;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

import java.util.ArrayList;
import java.util.List;

/** List of saved sites: the home page, YouTube, and whatever was added on the phone. */
final class SitesScreen extends Screen {
    // Android Auto limits how many rows a list may show; six is allowed everywhere.
    private static final int MAX_ROWS = 6;

    SitesScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        CarContext context = getCarContext();
        String home = Prefs.home(context);

        List<String> urls = new ArrayList<>();
        urls.add(home);
        if (!urls.contains(Prefs.DEFAULT_HOME)) {
            urls.add(Prefs.DEFAULT_HOME);
        }
        for (String site : Prefs.sites(context)) {
            if (!urls.contains(site)) {
                urls.add(site);
            }
        }

        ItemList.Builder list = new ItemList.Builder();
        for (int i = 0; i < urls.size() && i < MAX_ROWS; i++) {
            String url = urls.get(i);
            String title = i == 0 ? context.getString(R.string.car_home_row) : Prefs.label(url);
            list.addItem(new Row.Builder()
                    .setTitle(title)
                    .addText(url)
                    .setOnClickListener(() -> {
                        setResult(url);
                        finish();
                    })
                    .build());
        }

        return new ListTemplate.Builder()
                .setSingleList(list.build())
                .setTitle(context.getString(R.string.car_sites_title))
                .setHeaderAction(Action.BACK)
                .build();
    }
}
