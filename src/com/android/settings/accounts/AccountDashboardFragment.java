/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.accounts;

import static android.provider.Settings.EXTRA_AUTHORITIES;

import android.content.Context;
import android.icu.text.ListFormatter;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.text.BidiFormatter;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.users.AutoSyncDataPreferenceController;
import com.android.settings.users.AutoSyncPersonalDataPreferenceController;
import com.android.settings.users.AutoSyncWorkDataPreferenceController;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class AccountDashboardFragment extends DashboardFragment {

    private static final String TAG = "AccountDashboardFrag";


    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCOUNT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accounts_dashboard_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_user_and_account_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final String[] authorities = getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        return buildPreferenceControllers(context, this /* parent */, authorities);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            SettingsPreferenceFragment parent, String[] authorities) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final AccountPreferenceController accountPrefController =
                new AccountPreferenceController(context, parent, authorities);
        if (parent != null) {
            parent.getSettingsLifecycle().addObserver(accountPrefController);
        }
        controllers.add(accountPrefController);
        controllers.add(new AutoSyncDataPreferenceController(context, parent));
        controllers.add(new AutoSyncPersonalDataPreferenceController(context, parent));
        controllers.add(new AutoSyncWorkDataPreferenceController(context, parent));
        return controllers;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                final AuthenticatorHelper authHelper = new AuthenticatorHelper(mContext,
                        UserHandle.of(UserHandle.myUserId()), null /* OnAccountsUpdateListener */);
                final String[] types = authHelper.getEnabledAccountTypes();
                final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
                final List<CharSequence> summaries = new ArrayList<>();

                if (types == null || types.length == 0) {
                    summaries.add(mContext.getString(R.string.account_dashboard_default_summary));
                } else {
                    // Show up to 3 account types, ignore any null value
                    int accountToAdd = Math.min(3, types.length);

                    for (int i = 0; i < types.length && accountToAdd > 0; i++) {
                        final CharSequence label = authHelper.getLabelForType(mContext, types[i]);
                        if (TextUtils.isEmpty(label)) {
                            continue;
                        }

                        summaries.add(bidiFormatter.unicodeWrap(label));
                        accountToAdd--;
                    }
                }
                mSummaryLoader.setSummary(this, ListFormatter.getInstance().format(summaries));
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = (activity, summaryLoader) -> new SummaryProvider(activity, summaryLoader);

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.accounts_dashboard_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(
                            context, null /* parent */, null /* authorities*/);
                }
            };
}