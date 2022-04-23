package it.unipi.di.pantani.trashfinder;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import trashfinder.R;

public class FeedbackActivity extends AppCompatActivity {
    RadioGroup feedback_type;
    EditText feedback_text;
    Button feedback_send;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        feedback_type = findViewById(R.id.feedback_type);
        feedback_text = findViewById(R.id.feedback_text);
        feedback_send = findViewById(R.id.feedback_send);

        feedback_send.setOnClickListener(this::onClickSend);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private boolean validateForm() {
        boolean valid = true;

        int type = feedback_type.getCheckedRadioButtonId();
        if(type != R.id.feedback_type_error && type != R.id.feedback_type_suggestion) {
            ((RadioButton)findViewById(R.id.feedback_type_error)).setError(getResources().getString(R.string.feedback_type_field_error));
            ((RadioButton)findViewById(R.id.feedback_type_suggestion)).setError(getResources().getString(R.string.feedback_type_field_error));
            valid = false;
        }

        String text = feedback_text.getText().toString();
        if(text.length() < 10 || text.length() > 500) {
            feedback_text.setError(getResources().getString(R.string.feedback_text_error));
            valid = false;
        }

        return valid;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onClickSend(View view) {
        if(!validateForm()) return;

        // preparo intent
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(Uri.parse("mailto:"));
        i.putExtra(Intent.EXTRA_EMAIL, new String[] {Utils.FEEDBACK_MAIL});
        i.putExtra(Intent.EXTRA_TEXT, feedback_text.getText().toString());
        String subject;
        if(feedback_type.getCheckedRadioButtonId() == R.id.feedback_type_error) {
            subject = "Error";
        } else if(feedback_type.getCheckedRadioButtonId() == R.id.feedback_type_suggestion) {
            subject = "Suggestion";
        } else {
            subject = "Other";
        }
        i.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name) + " Feedback: " + subject);

        // avvio intent
        try {
            startActivity(Intent.createChooser(i, getResources().getString(R.string.feedback_chooser)));
        } catch(ActivityNotFoundException e) {
            Toast.makeText(FeedbackActivity.this, R.string.feedback_chooser_error, Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
