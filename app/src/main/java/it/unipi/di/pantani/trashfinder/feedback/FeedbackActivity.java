package it.unipi.di.pantani.trashfinder.feedback;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.databinding.ActivityFeedbackBinding;

public class FeedbackActivity extends AppCompatActivity {
    private ActivityFeedbackBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // applico il listener al pulsante "invio feedback"
        binding.feedbackSend.setOnClickListener(this::onClickSend);

        // metto nella actionbar la possibilità di tornare indietro
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private boolean validateForm() {
        boolean valid = true;

        int type = binding.feedbackType.getCheckedRadioButtonId();
        if(type != R.id.feedback_type_error && type != R.id.feedback_type_suggestion) {
            binding.feedbackTypeError.setError(getResources().getString(R.string.feedback_type_field_error));
            binding.feedbackTypeSuggestion.setError(getResources().getString(R.string.feedback_type_field_error));
            valid = false;
        }

        String text = binding.feedbackText.getText().toString();
        if(text.length() < 10 || text.length() > 500) {
            binding.feedbackText.setError(getResources().getString(R.string.feedback_text_error));
            valid = false;
        }

        return valid;
    }

    /**
     * Funzione che viene chiamata appena il pulsante "invia feedback" è premuto
     * @param notUsed la view cliccata (non usata in questo caso)
     */
    private void onClickSend(View notUsed) {
        if(!validateForm()) return;

        // preparo intent
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(Uri.parse("mailto:"));
        i.putExtra(Intent.EXTRA_EMAIL, new String[] {Utils.FEEDBACK_MAIL});
        i.putExtra(Intent.EXTRA_TEXT, binding.feedbackText.getText().toString());
        String subject;
        if(binding.feedbackType.getCheckedRadioButtonId() == R.id.feedback_type_error) {
            subject = "Error";
        } else if(binding.feedbackType.getCheckedRadioButtonId() == R.id.feedback_type_suggestion) {
            subject = "Suggestion";
        } else {
            subject = "Other";
        }
        i.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name) + " Feedback: " + subject);

        // avvio intent
        try {
            startActivity(Intent.createChooser(i, getResources().getString(R.string.feedback_chooser)));
        } catch(ActivityNotFoundException e) {
            Toast.makeText(this, R.string.feedback_chooser_error, Toast.LENGTH_LONG).show();
        }
        //finish();
    }

    // ---------------

    /*
        Se un pulsante delle opzioni è chiamato e quel pulsante è "home" torno indietro
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
