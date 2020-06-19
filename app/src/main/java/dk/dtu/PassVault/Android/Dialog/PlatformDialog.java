package dk.dtu.PassVault.Android.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dk.dtu.PassVault.Android.Adapter.AppListAdapter;
import dk.dtu.PassVault.R;

import static android.widget.Toast.LENGTH_LONG;

public class PlatformDialog extends DialogFragment {

    protected Context context;
    protected Listener listener;
    protected ArrayList<ApplicationInfo> packages = new ArrayList<>();
    private EditText mPlatformEditText;

    public interface Listener {
        void onDialogAddClick(DialogFragment dialog, String result);
        void onDialogCancelClick(DialogFragment dialog);
        void onDialogTouchOutsideClick(DialogFragment dialog);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
        this.listener = (Listener) context;

        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> resolveInfoList = this.context.getPackageManager().queryIntentActivities(intent, 0);

        for (ResolveInfo info : resolveInfoList) {
            if (info.activityInfo == null || info.activityInfo.applicationInfo == null) {
                continue;
            }

            ApplicationInfo appInfo = info.activityInfo.applicationInfo;

            if (appInfo.packageName != null && !appInfo.packageName.equals("dk.dtu.PassVault")) {
                this.packages.add(appInfo);
            }
        }

        PackageManager pm = this.context.getPackageManager();
        Collections.sort(this.packages, ((appInfo1, appInfo2) -> {
            String label1 = pm.getApplicationLabel(appInfo1).toString();
            String label2 = pm.getApplicationLabel(appInfo2).toString();

            return label1.compareTo(label2);
        }));
    }

    @Override
    public void onStop() {
        super.onStop();
        mPlatformEditText = (EditText) getActivity().findViewById(R.id.platform);
        mPlatformEditText.clearFocus();

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        setRetainInstance(true);

        builder.setView(inflater.inflate(R.layout.platform_dialog, null))
                .setPositiveButton("Add", (dialog, id) -> {

                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    listener.onDialogCancelClick(PlatformDialog.this);
                });

        AlertDialog ad = builder.create();

        ad.setOnShowListener(dialogInterface -> {
            AlertDialog dialog = (AlertDialog) dialogInterface;
            Button addBtn = (Button) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            RadioGroup radioGroup = (RadioGroup) ad.findViewById(R.id.radioGroup);
            Spinner spinner = (Spinner) ad.findViewById(R.id.appList);
            TextView webSiteText = (TextView) ad.findViewById(R.id.webSiteText);

            addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String result = "";
                    if (radioGroup.getCheckedRadioButtonId() == R.id.app) {
                        ApplicationInfo ai = (ApplicationInfo) spinner.getSelectedItem();
                        if (ai != null) {
                            result = "app://" + ai.packageName;
                            dialog.dismiss();
                        }
                    } else {
                        String input = webSiteText.getText().toString();
                        try {
                            String scheme = URI.create(input).getScheme();
                            if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
                                result = input;
                                dialog.dismiss();
                            } else {
                                Toast.makeText(getContext(), "No URL provided", LENGTH_LONG).show();
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                    listener.onDialogAddClick(PlatformDialog.this, result);
                }
            });

            dialog.setOnCancelListener(dialog1 -> listener.onDialogTouchOutsideClick(PlatformDialog.this));

            Spinner appList = (Spinner) dialog.findViewById(R.id.appList);

            if (appList != null) {
                appList.setAdapter(new AppListAdapter(this.context, R.layout.app_list_single, this.packages));
            }

            RadioButton radioButtonApp = (RadioButton) ad.findViewById(R.id.app);

            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    if (radioButtonApp.isChecked()) {
                        ad.findViewById(R.id.tabApp).setVisibility(View.VISIBLE);
                        ad.findViewById(R.id.tabWeb).setVisibility(View.INVISIBLE);
                    } else {
                        ad.findViewById(R.id.tabApp).setVisibility(View.INVISIBLE);
                        ad.findViewById(R.id.tabWeb).setVisibility(View.VISIBLE);
                    }
                }
            });

        });

        return ad;
    }

}