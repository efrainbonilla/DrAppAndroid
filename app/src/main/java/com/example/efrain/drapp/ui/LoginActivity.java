package com.example.efrain.drapp.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.efrain.drapp.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static com.example.efrain.drapp.util.LogUtils.*;


/**
 * A login screen that offers login via username|email/password.
 */
public class LoginActivity extends BaseActivity implements OnClickListener {
    private static final String TAG = makeLogTag(LoginActivity.class);
    // UI references.
    private EditText mEmailUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private LoginService loginService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = getActionBarToolbar();
        toolbar.setNavigationIcon(R.drawable.ic_launcher);

        mEmailUsernameView = (EditText) findViewById(R.id.txtEmailUsername);
        mPasswordView = (EditText) findViewById(R.id.txtPassword);

        Button mSignInBtn = (Button) findViewById(R.id.btnSignIn);
        mSignInBtn.setOnClickListener(this);

        mProgressView = (View) findViewById(R.id.login_progress);
        mLoginFormView = (View) findViewById(R.id.login_form);

        findViewById(R.id.linkLostPasswordView).setOnClickListener(this);
        findViewById(R.id.linkRegisterView).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSignIn:

                // Store values at the time of the login attempt.
                String email_username = mEmailUsernameView.getText().toString();
                String password = mPasswordView.getText().toString();

                boolean cancel = false;
                View focusView = null;

                if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
                    mPasswordView.setError(getString(R.string.error_invalid_password));
                    focusView = mPasswordView;
                    cancel = true;
                }
                if (TextUtils.isEmpty(email_username)) {
                    mEmailUsernameView.setError(getString(R.string.error_invalid_email_username));
                    focusView = mEmailUsernameView;
                    cancel = true;
                }

                if (cancel) {
                    focusView.requestFocus();
                } else {

                    showProgress(true);

                    loginService = new LoginService(email_username, password);
                    loginService.execute((Void) null);
                }

                break;

            case  R.id.linkLostPasswordView:
            case  R.id.linkRegisterView:
                Toast.makeText(this, "Clickme "+ v.getId(), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    public boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView
                    .animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(
                            new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                                }
                            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView
                    .animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(
                            new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                                }
                            });
        } else {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }

    }


    public class LoginService extends AsyncTask<Void, Void, Boolean> {

        private final String mEmailUsername;
        private final String mPassword;
        private final String apiURL;
        JSONObject responseJSON;

        LoginService(String email_username, String password) {
            mEmailUsername = email_username;
            mPassword = password;

            apiURL = getString(R.string.api_URL);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = true;
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(apiURL + "/login_check");
            httpPost.setHeader("Content-Type", "application/json");

            JSONObject data = new JSONObject();

            try {
                data.put("username", mEmailUsername);
                data.put("password", mPassword);

                StringEntity entity = new StringEntity(data.toString());

                httpPost.setEntity(entity);
                HttpResponse response = httpClient.execute(httpPost);
                String responseStr = EntityUtils.toString(response.getEntity());

                LOGD(TAG, "LoginService.doInBackground() response service: " + responseStr);

                JSONObject responseJson = new JSONObject(responseStr);

                if (!responseJson.has("token")) {
                    result = false;
                } else {
                    responseJSON = responseJson;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            loginService = null;
            showProgress(false);

            if (success) {
                try {

                    JSONObject data = responseJSON.getJSONObject("data");
                    Log.d(TAG, "data:username: " + data.getString("username"));

                    Intent intent = new Intent(LoginActivity.this, BrowseSessionsActivity.class);
                    startActivity(intent);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            loginService = null;
            showProgress(false);
        }
    }
}



