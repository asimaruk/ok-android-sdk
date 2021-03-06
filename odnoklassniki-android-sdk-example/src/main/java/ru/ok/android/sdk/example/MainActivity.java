package ru.ok.android.sdk.example;

import java.util.Collections;
import java.util.Currency;
import java.util.EnumSet;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import ru.ok.android.sdk.OKRestHelper;
import ru.ok.android.sdk.Odnoklassniki;
import ru.ok.android.sdk.OkAuthListener;
import ru.ok.android.sdk.OkListener;
import ru.ok.android.sdk.OkRequestMode;
import ru.ok.android.sdk.Shared;
import ru.ok.android.sdk.util.OkAuthType;
import ru.ok.android.sdk.util.OkDevice;
import ru.ok.android.sdk.util.OkScope;

public class MainActivity extends Activity {
    protected static final String APP_ID = "125497344";
    protected static final String APP_KEY = "CBABPLHIABABABABA";
    protected static final String REDIRECT_URL = "okauth://ok125497344";

    protected Odnoklassniki odnoklassniki;

    private View loginView;
    private View formView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginView = findViewById(R.id.login_block);

        Button mLoginBtn = (Button) findViewById(R.id.sdk_login_any);
        Button mLoginBtnSso = (Button) findViewById(R.id.sdk_login_sso);
        Button mLoginBtnOAuth = (Button) findViewById(R.id.sdk_login_oauth);
        mLoginBtn.setOnClickListener(new LoginClickListener(OkAuthType.ANY));
        mLoginBtnSso.setOnClickListener(new LoginClickListener(OkAuthType.NATIVE_SSO));
        mLoginBtnOAuth.setOnClickListener(new LoginClickListener(OkAuthType.WEBVIEW_OAUTH));

        formView = findViewById(R.id.sdk_form);
        findViewById(R.id.sdk_get_currentuser).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                odnoklassniki.requestAsync("users.getCurrentUser", null, null, new OkListener() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        Toast.makeText(MainActivity.this, "Get current user result: " + json.toString(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, "Get current user failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        findViewById(R.id.sdk_get_friends).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                new GetFriendsTask().execute();
            }
        });
        findViewById(R.id.sdk_logout).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                odnoklassniki.clearTokens();
                hideForm();
            }
        });

        findViewById(R.id.sdk_post).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String json = ("{'media':[" +
                        "{'type':'text', 'text':'Hello world!'}" + ',' +
                        "{'type':'link', 'url':'https://apiok.ru/'}" + ',' +
                        "{'type':'app', 'text':'Welcome from sample', 'images':[{'url':'https://apiok.ru/res/img/main/app_create.png'}], 'actions':[{'text': 'Play me!', 'mark': 'play_me_from_app_block'}]}" +
                        "]}").replaceAll("'", "\"");
                odnoklassniki.performPosting(MainActivity.this, json, false, null);
            }
        });
        findViewById(R.id.sdk_app_invite).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                odnoklassniki.performAppInvite(MainActivity.this, null);
            }
        });
        findViewById(R.id.sdk_app_suggest).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                odnoklassniki.performAppSuggest(MainActivity.this, null);
            }
        });
        findViewById(R.id.sdk_send_note).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new SdkSendNote().execute();
            }
        });
        findViewById(R.id.sdk_report_payment).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                odnoklassniki.reportPayment(Math.random() + "", "6.28", Currency.getInstance("EUR"));
            }
        });

        odnoklassniki = Odnoklassniki.createInstance(this, APP_ID, APP_KEY);
        odnoklassniki.checkValidTokens(new OkListener() {
            @Override
            public void onSuccess(JSONObject json) {
                showForm();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, String.format("%s: %s", getString(R.string.error), error), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Odnoklassniki.getInstance().isActivityRequestOAuth(requestCode)) {
            Odnoklassniki.getInstance().onAuthActivityResult(requestCode, resultCode, data, getAuthListener());
        } else if (Odnoklassniki.getInstance().isActivityRequestViral(requestCode)) {
            Odnoklassniki.getInstance().onActivityResultResult(requestCode, resultCode, data, getToastListener());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Creates a listener that displays result as a toast message
     */
    @NonNull
    private OkListener getToastListener() {
        return new OkListener() {
            @Override
            public void onSuccess(final JSONObject json) {
                Toast.makeText(MainActivity.this, json.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, String.format("%s: %s", getString(R.string.error), error), Toast.LENGTH_LONG).show();
            }
        };
    }

    /**
     * Creates a listener that is run on OAUTH authorization completion
     */
    @NonNull
    private OkAuthListener getAuthListener() {
        return new OkAuthListener() {
            @Override
            public void onSuccess(final JSONObject json) {
                try {
                    Toast.makeText(MainActivity.this,
                            String.format("access_token: %s", json.getString("access_token")),
                            Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                showForm();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this,
                        String.format("%s: %s", getString(R.string.error), error),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel(String error) {
                Toast.makeText(MainActivity.this,
                        String.format("%s: %s", getString(R.string.auth_cancelled), error),
                        Toast.LENGTH_SHORT).show();
            }
        };
    }

    protected final void showForm() {
        formView.setVisibility(View.VISIBLE);
        loginView.setVisibility(View.GONE);

        testIfInstallationSourceIsOK();
    }

    protected final void hideForm() {
        formView.setVisibility(View.GONE);
        loginView.setVisibility(View.VISIBLE);
    }

    // Using AsyncTask is arbitrary choice
    // Developers should do a better error handling job ;)

    protected final class GetFriendsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(final Void... params) {
            try {
                return odnoklassniki.request("friends.get", null, null);
            } catch (Exception exc) {
                Log.e("Odnoklassniki", "Failed to get friends", exc);
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String result) {
            if (result != null) {
                Toast.makeText(MainActivity.this, "Get user friends result: " + result, Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected class LoginClickListener implements OnClickListener {
        private OkAuthType authType;

        public LoginClickListener(OkAuthType authType) {
            this.authType = authType;
        }

        @Override
        public void onClick(final View view) {
            odnoklassniki.requestAuthorization(MainActivity.this, REDIRECT_URL, authType, OkScope.VALUABLE_ACCESS);
        }
    }

    private void testIfInstallationSourceIsOK() {
        Map<String, String> args = Collections.singletonMap("adv_id", OkDevice.getAdvertisingId(MainActivity.this));
        odnoklassniki.requestAsync("sdk.getInstallSource", args, EnumSet.of(OkRequestMode.UNSIGNED), new OkListener() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    int result = Integer.parseInt(json.optString("result"));
                    Toast.makeText(MainActivity.this,
                            (result > 0 ?
                                    "application installation caused by OK app (" + result + ")" :
                                    "application is not caused by OK app (" + result + ")"),
                            Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "invalid value while getting install source " + json, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "error while getting install source " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected final class SdkSendNote extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(final Void... params) {
            try {
                OKRestHelper helper = new OKRestHelper(odnoklassniki);
                if (!helper.sdkInit(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "sdk.sendNote: no sdk token available", Toast.LENGTH_SHORT).show();
                    return null;
                }
                JSONObject note = new JSONObject();
                note.put("uid", "12345678");
                note.put("image", "http://domain.tld/img.png");
                note.put("message", "Hello World from Android SDK!");
                note.put("payload", "SGVsbG8gd29ybGQhISE=");
                helper.sdkSendNote(note, new OkListener() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        String text = json == null ? null : json.toString();
                        Toast.makeText(MainActivity.this, "sdk.sendNote: OK " + text, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, "sdk.sendNote: ERROR " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(Shared.LOG_TAG, "sdk.sendNote " + e.getMessage(), e);
            }
            return null;
        }
    }
}
