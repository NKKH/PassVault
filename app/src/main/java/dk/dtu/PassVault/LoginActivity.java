package dk.dtu.PassVault;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.autofill.AutofillManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import dk.dtu.PassVault.Business.Crypto.Crypto;
import dk.dtu.PassVault.Business.Database.Database;
import dk.dtu.PassVault.Business.Database.Entities.Credential;
import dk.dtu.PassVault.Business.Service.AutoFillService;
import dk.dtu.PassVault.Business.Util.IconExtractor;

public class LoginActivity extends BaseActivity {

    private final String TAG = "Pass_Vault";
    private static int REQUEST_CODE_BUTTONS = 2;

    protected static class SetupCredentialTransaction extends Database.Transaction<Boolean> {
        protected WeakReference<Context> contextRef;
        protected String hashedPassword;

        SetupCredentialTransaction(WeakReference<Context> contextRef, String hashedPassword) {
            this.contextRef = contextRef;
            this.hashedPassword = hashedPassword;
        }

        @Override
        public Boolean doRequest(Database db) {
            Credential cred = new Credential(this.hashedPassword);
            db.setCredential(cred);

            return db.hasCredential();
        }

        @Override
        public void onResult(Boolean result) {
            Toast.makeText(
                    contextRef.get(),
                    result ? "Master password created!" : "Something went wrong!",
                    Toast.LENGTH_LONG
            ).show();
        }

    }

    protected static class DatabaseHasCredential extends Database.Transaction<Boolean> {

        protected WeakReference<LoginActivity> ref;

        public DatabaseHasCredential(WeakReference<LoginActivity> ref) {
            this.ref = ref;
        }

        @Override
        public Boolean doRequest(Database db) {
            return db.hasCredential();
        }

        @Override
        public void onResult(Boolean result) {
            LoginActivity activity = this.ref.get();

            if (activity == null) {
                return;
            }

            Button signInButton = activity.findViewById(R.id.sign_in_btn);
            Button registerButton = activity.findViewById(R.id.register_btn);

            Log.i("Login", String.valueOf(result));
            if (result) {
                signInButton.setEnabled(true);
                registerButton.setEnabled(false);
            } else {
                signInButton.setEnabled(false);
                registerButton.setEnabled(true);
            }
        }
    }

    // Allows activity to be started without a master password having been specified
    protected boolean allowNoKey() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        Button signInButton = findViewById(R.id.sign_in_btn);
        Button registerButton = findViewById(R.id.register_btn);


        signInButton.setOnClickListener(v -> {
            final EditText password = (EditText) findViewById(R.id.password);

            this.getCrypto().checkMasterPassword(
                    getApplicationContext(),
                    password.getText().toString(),
                    new Crypto.MasterPasswordValidationResponse() {
                        @Override
                        public void run() {
                            if (this.isValid) {
                                // Tell crypto instance about master password
                                this.crypto.setKey(password.getText().toString());
                                Intent intent = new Intent(getApplicationContext(), VaultActivity.class);
                                //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), "Wrong password entered.", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            );
        });


        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterMasterActivty.class);
            startActivityForResult(intent, REQUEST_CODE_BUTTONS);

            /*this.getCrypto().hash(password.getText().toString(), new Crypto.CryptoResponse() {
                @Override
                public void run() {
                    if(!this.isSuccessful) {
                        Toast.makeText(getApplicationContext(), "An error occurred!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Database.dispatch(
                            getApplicationContext(),
                            new SetupCredentialTransaction(
                                    new WeakReference<>(getApplicationContext()),
                                    this.hashedData
                            )
                    );
                }
            });*/
        });

        // Enable / Disable buttons
        DatabaseHasCredential transaction = new DatabaseHasCredential(new WeakReference<>(this));
        Database.dispatch(getApplicationContext(), transaction);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_BUTTONS) {
            // Enable / Disable buttons
            DatabaseHasCredential transaction = new DatabaseHasCredential(new WeakReference<>(this));
            Database.dispatch(getApplicationContext(), transaction);
        }
    }
}