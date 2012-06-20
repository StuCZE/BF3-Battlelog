/*
	This file is part of BF3 Battlelog

    BF3 Battlelog is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    BF3 Battlelog is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 */

/* TODO: Divide & conquer:
 *  # Create a Fragment per "branch" in the missionTree
 *      # Store types: LAYOUT_PAIR ; LAYOUT_TOPDOWN ; LAYOUT_TOPDOWN_LAST 
 */

package com.ninetwozero.battlelog.activity.profile.assignments;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.ninetwozero.battlelog.R;
import com.ninetwozero.battlelog.datatype.AssignmentData;
import com.ninetwozero.battlelog.datatype.AssignmentDataWrapper;
import com.ninetwozero.battlelog.datatype.ProfileData;
import com.ninetwozero.battlelog.datatype.WebsiteHandlerException;
import com.ninetwozero.battlelog.http.ProfileClient;
import com.ninetwozero.battlelog.http.RequestHandler;
import com.ninetwozero.battlelog.misc.Constants;
import com.ninetwozero.battlelog.misc.DataBank;
import com.ninetwozero.battlelog.misc.PublicUtils;
import com.ninetwozero.battlelog.misc.SessionKeeper;

public class AssignmentActivity extends Activity {

    // SharedPreferences for shizzle
    private SharedPreferences mSharedPreferences;
    private LayoutInflater mLayoutInflater;
    private ProfileData mProfileData;
    private Map<Long, AssignmentDataWrapper> mAssignments;
    private long mSelectedPersona;
    private int mSelectedPosition;
    private long[] mPersonaId;
    private String[] mPersonaName;
    private AssignmentData mCurrentPopupData;

    // Elements
    private TableLayout mTableAssignments;

    @Override
    public void onCreate(final Bundle icicle) {

        super.onCreate(icicle);

        // Set sharedPreferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        PublicUtils.setupFullscreen(this, mSharedPreferences);
        PublicUtils.setupSession(this, mSharedPreferences);
        PublicUtils.setupLocale(this, mSharedPreferences);

        setContentView(R.layout.assignment_view);

        if (!getIntent().hasExtra("profile")) {

            Toast.makeText(this, R.string.info_general_noprofile,
                    Toast.LENGTH_SHORT).show();
            finish();

        }
        mProfileData = (ProfileData) getIntent().getParcelableExtra("profile");

        initActivity();

    }

    public void initActivity() {

        // Prepare to tango
        mTableAssignments = (TableLayout) findViewById(R.id.table_assignments);

        // Let's try something out
        if (mProfileData.getId() == SessionKeeper.getProfileData().getId()) {

            mSelectedPersona = mSharedPreferences.getLong(Constants.SP_BL_PERSONA_CURRENT_ID, 0);
            mSelectedPosition = mSharedPreferences.getInt(Constants.SP_BL_PERSONA_CURRENT_POS, 0);

        } else {

            mSelectedPersona = mProfileData.getPersona(0).getId();

        }

    }

    @Override
    public void onResume() {

        super.onResume();

        // Setup the locale
        PublicUtils.setupLocale(this, mSharedPreferences);

        // Setup the session
        PublicUtils.setupSession(this, mSharedPreferences);

        // Reload the layout
        reload();

    }

    public void showAssignments(AssignmentDataWrapper data) {

        // Init to win it
        List<AssignmentData> b2kAssignments = data.getB2KAssignments();
        List<AssignmentData> premiumAssignments = data.getPremiumAssignments();
        List<AssignmentData> cqAssignments = data.getCQAssignments();

        // Let's clear the table
        mTableAssignments.removeAllViews();

        // Display B2K
        displayPairsInTable(mTableAssignments, b2kAssignments);
        displayStackedInTable(mTableAssignments, premiumAssignments);
        displayPairsInTable(mTableAssignments, cqAssignments);

    }

    // Loop & create
    public void displayPairsInTable(TableLayout table, List<AssignmentData> assignments) {

        for (int i = 0, max = assignments.size(); i < max; i += 2) {

            // Init the elements
            TableRow tableRow = (TableRow) mLayoutInflater.inflate(
                    R.layout.list_item_assignment, null);
            ProgressBar progressLeft = (ProgressBar) tableRow
                    .findViewById(R.id.progress_left);
            ProgressBar progressRight = (ProgressBar) tableRow
                    .findViewById(R.id.progress_right);
            ImageView imageLeft = (ImageView) tableRow
                    .findViewById(R.id.image_leftassignment);
            ImageView imageRight = (ImageView) tableRow
                    .findViewById(R.id.image_rightassignment);

            // Add the table row
            mTableAssignments.addView(tableRow);

            // Get the values
            AssignmentData ass1 = assignments.get(i);
            AssignmentData ass2 = assignments.get(i + 1);

            // Set the images
            imageLeft.setImageResource(ass1.getResourceId());
            if (ass1.isCompleted()) {

                imageRight.setImageResource(ass2.getResourceId());

            } else {

                imageRight.setImageResource(R.drawable.assignment_locked);

            }

            // Set the tags
            imageLeft.setTag(ass1); // i
            imageRight.setTag(ass2); // i+1

            // Get the progress...
            int progressValueLeft = ass1.getProgress();
            int progressValueRight = ass2.getProgress();

            // ...and set the progress bars
            progressLeft.setProgress(progressValueLeft);
            progressLeft.setMax(100);
            progressRight.setProgress(progressValueRight);
            progressRight.setMax(100);

        }

    }

    public void displayStackedInTable(TableLayout table, List<AssignmentData> assignments) {

        for (int i = 0, max = assignments.size(); i < max; i++) {

            // Get the data
            AssignmentData assignment = assignments.get(i);
            int progressValue = assignment.getProgress();

            // Init the elements
            TableRow tableRow = (TableRow) mLayoutInflater.inflate(
                    R.layout.list_item_assignment_stacked, null);
            ProgressBar progress = (ProgressBar) tableRow.findViewById(R.id.progress);
            ImageView image = (ImageView) tableRow.findViewById(R.id.image_assignment);

            // Act
            image.setImageResource(assignment.isCompleted()
                    ? assignment.getResourceId()
                    : R.drawable.assignment_locked);

            // ...and set the progress bars
            progress.setProgress(progressValue);
            progress.setMax(100);

            // Set the tags
            image.setTag(assignment);

            // Add the table row
            mTableAssignments.addView(tableRow);

        }

    }

    public void reload() {

        // ASYNC!!!
        new AsyncReload(this).execute(mProfileData);

    }

    public void doFinish() {
    }

    private class AsyncReload extends
            AsyncTask<ProfileData, Void, Boolean> {

        // Attributes
        private Context context;
        private ProgressDialog progressDialog;

        public AsyncReload(Context c) {

            context = c;

        }

        @Override
        protected void onPreExecute() {

            // Let's see if we got data already
            if (mAssignments == null) {

                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(context
                        .getString(R.string.general_wait));
                progressDialog
                        .setMessage(getString(R.string.general_downloading));
                progressDialog.show();

            }

        }

        @Override
        protected Boolean doInBackground(ProfileData... arg0) {

            try {

                ProfileClient profileHandler = new ProfileClient(arg0[0]);
                mAssignments = profileHandler.getAssignments(context);
                return (mAssignments != null);

            } catch (WebsiteHandlerException ex) {

                ex.printStackTrace();
                return false;

            }

        }

        @Override
        protected void onPostExecute(Boolean result) {

            // Fail?
            if (!result) {

                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                Toast.makeText(context, R.string.general_no_data,
                        Toast.LENGTH_SHORT).show();
                ((Activity) context).finish();

            }

            // Do actual stuff
            showAssignments(mAssignments.get(mSelectedPersona));

            // Go go go
            if (progressDialog != null) {
                progressDialog.dismiss();
            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_unlock, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Let's act!
        if (item.getItemId() == R.id.option_reload) {

            reload();

        } else if (item.getItemId() == R.id.option_change) {

            generateDialogPersonaList().show();

        } else if (item.getItemId() == R.id.option_back) {

            ((Activity) this).finish();

        }

        // Return true yo
        return true;

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(Constants.SUPER_COOKIES,
                RequestHandler.getCookies());

    }

    @Override
    protected Dialog onCreateDialog(int id) {

        // Init
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialog = mLayoutInflater.inflate(R.layout.popup_dialog_view, null);
        LinearLayout wrapObjectives = (LinearLayout) dialog.findViewById(R.id.wrap_objectives);

        // Set the title
        builder.setCancelable(false).setPositiveButton("OK",

                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {

                        dialog.dismiss();

                    }

                });

        // Create the dialog and set the contentView
        builder.setView(dialog);
        builder.setCancelable(true);

        // Grab the data
        String[] assignmentTitleData = DataBank.getAssignmentTitle(mCurrentPopupData.getId());

        // Set the actual fields too
        ImageView imageAssignment = ((ImageView) dialog.findViewById(R.id.image_assignment));
        imageAssignment.setImageResource(mCurrentPopupData.getResourceId());

        // turn off clickable in assignment dialog (image_assignment needs it in
        // the assignment list window)
        imageAssignment.setClickable(false);
        ((TextView) dialog.findViewById(R.id.text_title))
                .setText(assignmentTitleData[0]);

        // Loop over the criterias
        for (AssignmentData.Objective objective : mCurrentPopupData.getObjectives()) {

            // Inflate a layout...
            View v = mLayoutInflater.inflate(
                    R.layout.list_item_assignment_popup, null);

            // ...and set the fields
            ((TextView) v.findViewById(R.id.text_obj_title)).setText(DataBank
                    .getAssignmentCriteria(objective.getDescription()));
            ((TextView) v.findViewById(R.id.text_obj_values)).setText(

                    objective.getCurrentValue() + "/" + objective.getGoalValue()

                    );

            wrapObjectives.addView(v);

        }

        ((ImageView) dialog.findViewById(R.id.image_reward))
                .setImageResource(mCurrentPopupData.getUnlockResourceId());
        ((TextView) dialog.findViewById(R.id.text_rew_name))
                .setText(assignmentTitleData[1]);

        AlertDialog theDialog = builder.create();
        theDialog.setView(dialog, 0, 0, 0, 0);

        return theDialog;
    }

    public void onPopupClick(View v) {

        mCurrentPopupData = (AssignmentData) v.getTag();
        showDialog(0);

    }

    public Dialog generateDialogPersonaList() {

        // Attributes
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set the title and the view
        builder.setTitle(R.string.info_dialog_soldierselect);

        // Do we have items to show?
        if (mPersonaId == null) {

            // Init
            mPersonaId = new long[mProfileData.getNumPersonas()];
            mPersonaName = new String[mProfileData.getNumPersonas()];

            // Iterate
            for (int count = 0, max = mPersonaId.length; count < max; count++) {

                mPersonaId[count] = mProfileData.getPersona(count).getId();
                mPersonaName[count] = mProfileData.getPersona(count).getName();

            }

        }

        // Set it up
        builder.setSingleChoiceItems(

                mPersonaName, -1, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {

                        if (mPersonaId[item] != mSelectedPersona) {

                            // Update it
                            mSelectedPersona = mPersonaId[item];

                            // Store selectedPersonaPos
                            mSelectedPosition = item;

                            // Update the layout
                            showAssignments(mAssignments.get(mSelectedPersona));

                            // Save it
                            if (mProfileData.getId() == SessionKeeper.getProfileData().getId()) {
                                SharedPreferences.Editor spEdit = mSharedPreferences.edit();
                                spEdit.putLong(Constants.SP_BL_PERSONA_CURRENT_ID, mSelectedPersona);
                                spEdit.putInt(Constants.SP_BL_PERSONA_CURRENT_POS,
                                        mSelectedPosition);
                                spEdit.commit();
                            }

                        }

                        dialog.dismiss();

                    }

                }

                );

        // CREATE
        return builder.create();

    }
}
