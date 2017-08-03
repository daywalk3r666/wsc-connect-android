package wscconnect.android.fragments.myApps;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import wscconnect.android.GlideApp;
import wscconnect.android.R;
import wscconnect.android.Utils;
import wscconnect.android.activities.MainActivity;
import wscconnect.android.adapters.AppOptionAdapter;
import wscconnect.android.callbacks.RetroCallback;
import wscconnect.android.fragments.myApps.appOptions.AppWebviewFragment;
import wscconnect.android.listeners.OnTokenUpdateListener;
import wscconnect.android.models.AccessTokenModel;
import wscconnect.android.models.AppOptionModel;

/**
 * Created by chris on 18.07.17.
 */

public class AppOptionsFragment extends Fragment implements OnTokenUpdateListener {
    public static final String OPTION_TYPE_WEBVIEW = "optionWebview";
    public static final String OPTION_TYPE_NOTIFICATIONS = "optionNotifications";
    public static final String OPTION_TYPE_MESSAGES = "optionMessages";
    public AccessTokenModel token;
    private MainActivity activity;
    private RecyclerView recyclerView;
    private List<AppOptionModel> optionList;
    private AppOptionAdapter appOptionAdapter;
    private FrameLayout contentView;
    private String webviewFragmentTag;
    private FragmentManager fManager;
    private LinearLayout optionsLayoutView;
    private ImageView accountAvatarView;
    private TextView accountUsernameView;
    private TextView accountAppNameView;
    private ImageButton accountOverflowView;

    public AppOptionsFragment() {

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(MainActivity.TAG, (savedInstanceState == null) ? "onActivityCreated savedInstanceState == null" : "onActivitCreated savedInstanceState != null");
        if (savedInstanceState != null) {
            Log.i(MainActivity.TAG, "savedInstanceState token = " + savedInstanceState.getParcelable(AccessTokenModel.EXTRA));
        }

        token = getArguments().getParcelable(AccessTokenModel.EXTRA);

        activity = (MainActivity) getActivity();
        fManager = getChildFragmentManager();
        optionList = new ArrayList<>();
        appOptionAdapter = new AppOptionAdapter(activity, optionList, token, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(appOptionAdapter);

        webviewFragmentTag = AppWebviewFragment.class.getSimpleName();

        accountOverflowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final PopupMenu popup = new PopupMenu(activity, accountOverflowView);
                popup.getMenuInflater().inflate(R.menu.account_popup_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.account_popup_menu_logout:
                                logout();
                                break;
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        accountAvatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showFullScreenPhotoDialog(activity, token.getAvatar());
            }
        });

        loadUser();
        loadOptions();
    }

    private void loadUser() {
        Log.i(MainActivity.TAG, "loadUser username:" + token.getUsername() + " appID " + token.getAppID());
        accountUsernameView.setText(token.getUsername());
        accountAppNameView.setText(token.getAppName());
        GlideApp.with(activity).load(token.getAvatar()).error(R.drawable.ic_person_black_50dp).circleCrop().into(accountAvatarView);
    }

    public void loadOptions() {
        AppOptionModel webviewOption = new AppOptionModel();
        webviewOption.setTitle(token.getAppUrl());
        webviewOption.setIconUrl(token.getAppLogo());
        webviewOption.setMoreIcon(R.drawable.ic_chevron_right_black_24dp);
        webviewOption.setType(OPTION_TYPE_WEBVIEW);

        AppOptionModel notificationsOption = new AppOptionModel();
        notificationsOption.setTitle(getString(R.string.fragment_app_options_notifications, token.getAppName()));
        notificationsOption.setIcon(R.drawable.ic_notifications_black_24dp);
        notificationsOption.setMoreIcon(R.drawable.ic_keyboard_arrow_down_black_24dp);
        notificationsOption.setType(OPTION_TYPE_NOTIFICATIONS);

        AppOptionModel messageOptions = new AppOptionModel();
        messageOptions.setTitle(getString(R.string.fragment_app_options_messages));
        messageOptions.setIcon(R.drawable.ic_view_list_black_24dp);
        messageOptions.setMoreIcon(R.drawable.ic_keyboard_arrow_down_black_24dp);
        messageOptions.setType(OPTION_TYPE_MESSAGES);

        optionList.clear();
        optionList.add(webviewOption);
        optionList.add(notificationsOption);
        optionList.add(messageOptions);

        appOptionAdapter.notifyDataSetChanged();
    }

    @Override
    public void onUpdate(AccessTokenModel token) {
        Log.i(MainActivity.TAG, "onUpdate with new token: " + token.getAppName() + " " + token.getUsername());
        Log.i(MainActivity.TAG, "onUpdate oldToken is: " + this.token.getAppName() + " " + this.token.getUsername());
        this.token = token;
        loadUser();
        appOptionAdapter.removeFragments();
        loadOptions();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Log.i(MainActivity.TAG,"onPause");
        appOptionAdapter.removeFragments();
        loadOptions();
    }

    private void logout() {
        final ProgressBar progressBar = Utils.showProgressView(activity, accountOverflowView, android.R.attr.progressBarStyleSmall);
        activity.getAPI(token.getToken()).logout(token.getAppID()).enqueue(new RetroCallback<ResponseBody>(activity) {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                super.onResponse(call, response);

                // we ignore errors and just log the user out
                Utils.hideProgressView(accountOverflowView, progressBar);
                Utils.removeAccessTokenString(activity, token.getAppID());

                activity.updateAppsFragment();
                activity.updateMyAppsFragment();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                super.onFailure(call, t);

                Utils.hideProgressView(accountOverflowView, progressBar);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_app_options, container, false);

        optionsLayoutView = (LinearLayout) view.findViewById(R.id.fragment_app_options_options);
        recyclerView = (RecyclerView) view.findViewById(R.id.fragment_app_options_list);
        contentView = (FrameLayout) view.findViewById(R.id.fragment_app_options_content);
        accountAvatarView = (ImageView) view.findViewById(R.id.fragment_app_options_account_avatar);
        accountUsernameView = (TextView) view.findViewById(R.id.fragment_app_options_account_username);
        accountAppNameView = (TextView) view.findViewById(R.id.fragment_app_options_account_app_name);
        accountOverflowView = (ImageButton) view.findViewById(R.id.fragment_app_options_account_overflow);

        return view;
    }

    public void hideWebview() {
        contentView.setVisibility(View.GONE);
        optionsLayoutView.setVisibility(View.VISIBLE);
    }

    public void showOption(String type) {
        showOption(type, null);
    }

    public void showOption(String type, String webviewUrl) {
        switch (type) {
            case OPTION_TYPE_NOTIFICATIONS:
                // TODO not working right now
                //appOptionAdapter.performOptionClick(OPTION_TYPE_NOTIFICATIONS);
                AppOptionAdapter.MyViewHolder vH = (AppOptionAdapter.MyViewHolder) recyclerView.findViewHolderForAdapterPosition(1);

                if (vH == null)
                loadOptions();
                vH = (AppOptionAdapter.MyViewHolder) recyclerView.findViewHolderForAdapterPosition(1);

                Log.i("vHd", (vH == null) ? "vH == null" : "vh != null");
                vH.itemView.performClick();
                break;
            case OPTION_TYPE_WEBVIEW:
                Fragment fragment;
                FragmentTransaction transaction = fManager.beginTransaction();

                fragment = fManager.findFragmentByTag(webviewFragmentTag);
                if (fragment != null) {
                    fManager.beginTransaction().show(fragment).commit();
                    if (webviewUrl != null) {
                        ((AppWebviewFragment) fragment).setUrl(webviewUrl);
                        ((AppWebviewFragment) fragment).loadWebview();
                    }
                } else {
                    fragment = new AppWebviewFragment();

                    Bundle bundle = new Bundle();
                    bundle.putParcelable(AccessTokenModel.EXTRA, token);
                    if (webviewUrl != null) {
                        bundle.putString("webviewUrl", webviewUrl);
                    }
                    fragment.setArguments(bundle);
                    transaction.add(R.id.fragment_app_options_content, fragment, webviewFragmentTag).commit();
                    transaction.addToBackStack(null);
                }

                optionsLayoutView.setVisibility(View.GONE);
                contentView.setVisibility(View.VISIBLE);
                break;
        }
    }

    public boolean onBackPressed() {
        AppWebviewFragment fragment = (AppWebviewFragment) fManager.findFragmentByTag(webviewFragmentTag);

        if (contentView != null && contentView.getVisibility() == View.VISIBLE && fragment != null) {
            if (!fragment.goBackWebview()) {
                contentView.setVisibility(View.GONE);
                optionsLayoutView.setVisibility(View.VISIBLE);
            }

            return true;
        }

        return false;
    }

    public void resetViews() {
        if (contentView != null && optionsLayoutView != null) {
            contentView.setVisibility(View.GONE);
            optionsLayoutView.setVisibility(View.VISIBLE);
        }
    }
}