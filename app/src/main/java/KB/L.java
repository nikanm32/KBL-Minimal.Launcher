package KB;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class L extends Activity implements View.OnClickListener, View.OnLongClickListener {

    final String key_android = "android";
    final String key_status = "statusbar";
    final String key_status_pkg = key_android+".app.StatusBarManager";
    final String key_expand_status = "expandNotificationsPanel";
    final String key_dot = ".";
    final String key_empty = "";

    final String key_pref = BuildConfig.APPLICATION_ID;
    final String app_name = "N";
    final String app_package = "P";
    final String app_user_handle = "U";

    final String value_myket_url = BuildConfig.m + key_pref;
    final String value_indicator = "__";
    final String value_app = "App";
    final String value_default = "Set "+L.class.getName()+" as default launcher";
    final String value_about = "Based on 'OLauncher' GPL-v3 Project\n" +
            "NikanApps products, don't forget rate ;)";
    final String value_app_not_found = value_app + " not found";
    final String value_unable_launch_app = "Unable to launch " + value_app;
    final String value_search = "Search for launcher or home "+value_app+"s";
    final String value_long_press = "Long press to select "+value_app;


    final int FLAG_LAUNCH_APP = 0;
    final List<AppModel> appList = new ArrayList<>();

    boolean inCredit;
    Prefs prefs;
    View appDrawer;
    EditText search;
    ListView appListView;
    AppAdapter appAdapter;
    LinearLayout homeAppsLayout;
    TextView homeApp1, homeApp2, homeApp3, homeApp4, homeApp5, homeApp6, setDefaultLauncher;

    interface AppClickListener {
        void appClicked(AppModel appModel, int flag);

        void appLongPress(AppModel appModel);
    }

    @Override
    public void onBackPressed() {
        if (appDrawer.getVisibility() == View.VISIBLE) backToHome();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        findViewById(R.id.l_m).setOnTouchListener(getSwipeGestureListener());

        prefs = new Prefs();
        search = (EditText) findViewById(R.id.e_s);
        homeAppsLayout = (LinearLayout) findViewById(R.id.l_apps);
        appDrawer = findViewById(R.id.l_drawer);

        appAdapter = new AppAdapter(this, appList, getAppClickListener());
        appListView = (ListView) findViewById(R.id.lv_apps);
        appListView.setAdapter(appAdapter);

        initClickListeners();

        search.setHint(value_indicator);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                appAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        appListView.setOnScrollListener(getScrollListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
        backToHome();
        populateHomeApps();
        refreshAppsList();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.t_d) {
            if (inCredit) rateApp();
            else resetDefaultLauncher();
            return;
        }
        try {
            int location = Integer.parseInt(view.getTag().toString());
            homeAppClicked(location);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void rateApp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(value_myket_url)));
        } catch (ActivityNotFoundException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        try {
            int location = Integer.parseInt(view.getTag().toString());
            showAppList(location);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    TextView initTV(int i) {
        TextView tv = (TextView) homeAppsLayout.getChildAt(i - 1);
        tv.setTag(i+key_empty);
        tv.setHint(value_app);
        tv.setOnClickListener(this);
        tv.setOnLongClickListener(this);
        return tv;
    }

    void initClickListeners() {
        setDefaultLauncher = (TextView) findViewById(R.id.t_d);
        setDefaultLauncher.setOnClickListener(this);

        homeApp1 = initTV(1);
        homeApp2 = initTV(2);
        homeApp3 = initTV(3);
        homeApp4 = initTV(4);
        homeApp5 = initTV(5);
        homeApp6 = initTV(6);
    }

    void populateHomeApps() {
        homeApp1.setText(prefs.getAppName(1));
        homeApp2.setText(prefs.getAppName(2));
        homeApp3.setText(prefs.getAppName(3));
        homeApp4.setText(prefs.getAppName(4));
        homeApp5.setText(prefs.getAppName(5));
        homeApp6.setText(prefs.getAppName(6));
    }

    void showLongPressToast() {
        sToast(value_long_press);
    }

    void backToHome() {
        appDrawer.setVisibility(View.GONE);
        homeAppsLayout.setVisibility(View.VISIBLE);
        appAdapter.setFlag(FLAG_LAUNCH_APP);
        hideKeyboard();
        appListView.setSelectionAfterHeaderView();
        checkForDefaultLauncher();
    }

    void refreshAppsList() {
        new Thread(() -> {
            try {
                List<AppModel> apps = new ArrayList<>();
                UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
                LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
                for (UserHandle profile : userManager.getUserProfiles()) {
                    for (LauncherActivityInfo activityInfo : launcherApps.getActivityList(null, profile))
                        if (!activityInfo.getApplicationInfo().packageName.equals(BuildConfig.APPLICATION_ID))
                            apps.add(new AppModel(
                                    activityInfo.getLabel().toString(),
                                    activityInfo.getApplicationInfo().packageName,
                                    profile));
                }
                Collections.sort(apps, (app1, app2) -> app1.appLabel.compareToIgnoreCase(app2.appLabel));
                appList.clear();
                appList.addAll(apps);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    void showAppList(int flag) {
        setDefaultLauncher.setVisibility(View.GONE);
        showKeyboard();
        search.setText(key_empty);
        appAdapter.setFlag(flag);
        homeAppsLayout.setVisibility(View.GONE);
        appDrawer.setVisibility(View.VISIBLE);
    }

    void showKeyboard() {
        search.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(search, InputMethodManager.SHOW_FORCED);
    }

    void hideKeyboard() {
        search.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    void expandNotificationDrawer() {
        try {
            Object statusBarService = getSystemService(key_status);
            Class<?> statusBarManager = Class.forName(key_status_pkg);
            Method method = statusBarManager.getMethod(key_expand_status);
            method.invoke(statusBarService);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void prepareToLaunchApp(AppModel appModel) {
        hideKeyboard();
        launchApp(appModel);
        backToHome();
        search.setText(key_empty);
    }

    void homeAppClicked(int location) {
        if (prefs.getAppPackage(location).isEmpty()) showLongPressToast();
        else launchApp(getAppModel(
                prefs.getAppName(location),
                prefs.getAppPackage(location),
                prefs.getAppUserHandle(location)));
    }

    void launchApp(AppModel appModel) {
        LauncherApps launcher = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
        List<LauncherActivityInfo> appLaunchActivityList = launcher.getActivityList(appModel.appPackage, appModel.userHandle);
        ComponentName componentName;

        switch (appLaunchActivityList.size()) {
            case 0:
                sToast(value_app_not_found);
                return;
            case 1:
                componentName = new ComponentName(appModel.appPackage, appLaunchActivityList.get(0).getName());
                break;
            default:
                componentName = new ComponentName(
                        appModel.appPackage, appLaunchActivityList.get(appLaunchActivityList.size() - 1).getName());
                break;
        }

        try {
            launcher.startMainActivity(componentName, appModel.userHandle, null, null);
        } catch (SecurityException securityException) {
            launcher.startMainActivity(componentName, android.os.Process.myUserHandle(), null, null);
        } catch (Exception e) {
            sToast(value_unable_launch_app);
        }
    }

    void openAppInfo(AppModel appModel) {
        LauncherApps launcher = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
        Intent intent = getPackageManager().getLaunchIntentForPackage(appModel.appPackage);
        if (intent == null || intent.getComponent() == null) return;
        launcher.startAppDetailsActivity(intent.getComponent(), appModel.userHandle, null, null);
    }

    void setHomeApp(AppModel appModel, int flag) {
        prefs.setHomeApp(appModel, flag);
        backToHome();
        populateHomeApps();
    }

    void checkForDefaultLauncher() {
        if (BuildConfig.APPLICATION_ID.equals(getDefaultLauncherPackage()))
            setDefaultLauncher.setVisibility(View.GONE);
        else {
            setDefaultLauncher.setVisibility(View.VISIBLE);
            setDefaultLauncher.setText(value_default);
        }
    }

    String getDefaultLauncherPackage() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo result = getPackageManager().resolveActivity(intent, 0);
        if (result == null || result.activityInfo == null)
            return key_android;
        return result.activityInfo.packageName;
    }

    void resetDefaultLauncher() {
        try {
            PackageManager packageManager = getPackageManager();
            ComponentName componentName = new ComponentName(this, F.class);
            packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
            packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (getDefaultLauncherPackage().contains(key_dot))
            openLauncherPhoneSettings();
    }

    void openLauncherPhoneSettings() {
        /*if (Build.VERSION.SDK_INT >= 24) {
            sToast(value_default);
            startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        } else */{
            sToast(value_search);
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    AppClickListener getAppClickListener() {
        return new AppClickListener() {
            @Override
            public void appClicked(AppModel appModel, int flag) {
                if (flag == FLAG_LAUNCH_APP) prepareToLaunchApp(appModel);
                else setHomeApp(appModel, flag);
            }

            @Override
            public void appLongPress(AppModel appModel) {
                hideKeyboard();
                openAppInfo(appModel);
            }
        };
    }

    AppModel getAppModel(String appLabel, String appPackage, String appUserHandle) {
        return new AppModel(appLabel, appPackage, getUserHandleFromString(appUserHandle));
    }

    UserHandle getUserHandleFromString(String appUserHandleString) {
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        for (UserHandle userHandle : userManager.getUserProfiles())
            if (userHandle.toString().equals(appUserHandleString))
                return userHandle;
        return android.os.Process.myUserHandle();
    }

    AbsListView.OnScrollListener getScrollListener() {
        return new AbsListView.OnScrollListener() {

            boolean onTop = false;

            @Override
            public void onScrollStateChanged(AbsListView listView, int state) {
                if (state == 1) { // dragging
                    onTop = !listView.canScrollVertically(-1);
                    if (onTop) hideKeyboard();

                } else if (state == 0) { // stopped
                    if (!listView.canScrollVertically(1)) hideKeyboard();
                    else if (!listView.canScrollVertically(-1)) {
                        if (onTop) backToHome();
                        else showKeyboard();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {

            }
        };
    }

    View.OnTouchListener getSwipeGestureListener() {
        return new OnSwipeTouchListener() {
            @Override
            void onSwipeLeft() {
                super.onSwipeLeft();
                Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                startActivity(intent);
            }

            @Override
            void onSwipeRight() {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                startActivity(intent);
            }

            @Override
            void onSwipeUp() {
                showAppList(FLAG_LAUNCH_APP);
            }

            @Override
            void onSwipeDown() {
                expandNotificationDrawer();
            }

            @Override
            void onLongClick() {
                runOnUiThread(L.this::changeHomeAppAlignment);
            }

            @Override
            void onDoubleClick() {
                inCredit = true;
                setDefaultLauncher.setText(value_about);
                setDefaultLauncher.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    inCredit = false;
                    runOnUiThread(L.this::checkForDefaultLauncher);
                }, 2500);
            }

        };
    }

    int align = Gravity.START;
    int gravity = Gravity.CENTER;

    void changeHomeAppAlignment() {
        switch (align) {
            case Gravity.CENTER:
                gravity = Gravity.END;
                break;
            case Gravity.END:
                gravity = Gravity.START;
                break;
            default:
                gravity = Gravity.CENTER;
        }
        homeAppsLayout.setGravity(gravity);
        align = gravity;
        appAdapter.notifyDataSetChanged();
    }

    class AppModel {
        String appLabel;
        String appPackage;
        UserHandle userHandle;

        AppModel(String appLabel, String appPackage, UserHandle userHandle) {
            this.appLabel = appLabel;
            this.appPackage = appPackage;
            this.userHandle = userHandle;
        }
    }

    class AppAdapter extends BaseAdapter implements Filterable {

        final Context context;
        final AppClickListener appClickListener;
        List<AppModel> filteredAppsList;
        final List<AppModel> allAppsList;
        int flag = 0;

        class ViewHolder {
            TextView appName;
            View indicator;
        }

        AppAdapter(Context context, List<AppModel> apps, AppClickListener appClickListener) {
            this.context = context;
            this.appClickListener = appClickListener;
            this.filteredAppsList = apps;
            this.allAppsList = apps;
        }

        public void setFlag(int flag) {
            this.flag = flag;
        }

        @Override
        public int getCount() {
            return filteredAppsList.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredAppsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppModel appModel = (AppModel) getItem(position);
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.adapter_app, parent, false);
                viewHolder.appName = (TextView) ((ViewGroup) convertView).getChildAt(0);
                viewHolder.indicator = (TextView) ((ViewGroup) convertView).getChildAt(1);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.appName.setTag(appModel);
            viewHolder.appName.setText(appModel.appLabel);
            viewHolder.appName.setOnClickListener(view -> {
                AppModel clickedAppModel = (AppModel) viewHolder.appName.getTag();
                appClickListener.appClicked(clickedAppModel, flag);
            });
            viewHolder.appName.setOnLongClickListener(view -> {
                AppModel clickedAppModel = (AppModel) viewHolder.appName.getTag();
                appClickListener.appLongPress(clickedAppModel);
                return true;
            });
            if (appModel.userHandle == android.os.Process.myUserHandle())
                viewHolder.indicator.setVisibility(View.GONE);
            else viewHolder.indicator.setVisibility(View.VISIBLE);

            if (flag == 0 && getCount() == 1) appClickListener.appClicked(appModel, flag);

            return convertView;
        }

        @Override
        public Filter getFilter() {

            return new Filter() {
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredAppsList = (List<AppModel>) results.values;
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<AppModel> filteredApps = new ArrayList<>();

                    if (constraint.toString().isEmpty())
                        filteredApps = allAppsList;
                    else {
                        constraint = constraint.toString().toLowerCase();
                        for (int i = 0; i < allAppsList.size(); i++) {
                            AppModel app = allAppsList.get(i);
                            if (app.appLabel.toLowerCase().contains(constraint))
                                filteredApps.add(app);
                        }
                    }

                    results.count = filteredApps.size();
                    results.values = filteredApps;
                    return results;
                }
            };
        }
    }

    public class OnSwipeTouchListener implements View.OnTouchListener {

        boolean longPressOn = false;
        final GestureDetector gestureDetector;

        OnSwipeTouchListener() {
            gestureDetector = new GestureDetector(L.this, new GestureListener());
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) longPressOn = false;
            return gestureDetector.onTouchEvent(motionEvent);
        }

        class GestureListener extends GestureDetector.SimpleOnGestureListener {

            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return super.onSingleTapUp(motionEvent);
            }

            @Override
            public boolean onDoubleTap(MotionEvent motionEvent) {
                onDoubleClick();
                return super.onDoubleTap(motionEvent);
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
                longPressOn = true;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (longPressOn) onLongClick();
                    }
                }, 500);
                super.onLongPress(motionEvent);
            }

            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
                try {
                    float diffY = event2.getY() - event1.getY();
                    float diffX = event2.getX() - event1.getX();
                    int SWIPE_VELOCITY_THRESHOLD = 100;
                    int SWIPE_THRESHOLD = 100;
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD)
                            if (diffX > 0) onSwipeRight();
                            else onSwipeLeft();
                    } else {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD)
                            if (diffY < 0) onSwipeUp();
                            else onSwipeDown();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        }

        void onSwipeRight() {
        }

        void onSwipeLeft() {
        }

        void onSwipeUp() {
        }

        void onSwipeDown() {
        }

        void onLongClick() {
        }

        void onDoubleClick() {
        }

    }

    class Prefs {

        String getString(String key) {
            return getSharedPref().getString(key, key_empty);
        }

        void setString(String key, String value) {
            getSharedPref().edit().putString(key, value).apply();
        }

        final SharedPreferences sharedPreferences;

        Prefs() {
            sharedPreferences = getSharedPreferences(key_pref, Context.MODE_PRIVATE);
        }

        SharedPreferences getSharedPref() {
            return sharedPreferences;
        }

        String getAppName(int location) {
            return getString(app_name + location);
        }

        String getAppPackage(int location) {
            return getString(app_package + location);
        }

        String getAppUserHandle(int location) {
            return getString(app_user_handle + location);
        }

        void setHomeApp(L.AppModel app, int location) {
            setString(app_name + location, app.appLabel);
            setString(app_package + location, app.appPackage);
            setString(app_user_handle + location, app.userHandle.toString());
        }
    }

    public static class F extends Activity { }

    void sToast(String s) { Toast.makeText(this, s, 0).show(); }
}