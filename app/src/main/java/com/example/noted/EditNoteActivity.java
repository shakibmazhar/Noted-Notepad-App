package com.example.noted;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.example.noted.db.NotesDB;
import com.example.noted.db.NotesDao;
import com.example.noted.model.Note;

import java.util.Date;

public class EditNoteActivity extends AppCompatActivity {
    private EditText inputTitle;
    private EditText inputNote;
    private NotesDao dao;
    private Note temp;
    public static final String NOTE_EXTRA_KEY = "note_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        inputTitle = findViewById(R.id.input_title);
        inputNote = findViewById(R.id.input_note);
        dao = NotesDB.getInstance(this).notesDao();
        if (getIntent().getExtras()!= null){
            int id = getIntent().getExtras().getInt(NOTE_EXTRA_KEY, 0);
            temp = dao.getNoteById(id);
            inputTitle.setText(temp.getNoteTitle());
            inputNote.setText(temp.getNoteText());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_note_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.save_note)
            onSaveNote();
        return super.onOptionsItemSelected(item);
    }

    private void onSaveNote() {
        String title = inputTitle.getText().toString();
        String text = inputNote.getText().toString();
        if (!text.isEmpty() || !title.isEmpty()) {
            long date = new Date().getTime();
            if (temp == null) {
                temp = new Note(title, text, date);
                dao.insertNote(temp);
                Toast.makeText(this, "Note Saved!", Toast.LENGTH_SHORT).show();
            }
            else {
                temp.setNoteTitle(title);
                temp.setNoteText(text);
                temp.setNoteDate(date);
                dao.updateNote(temp);
                Toast.makeText(this, "Note Saved!", Toast.LENGTH_SHORT).show();
            }
        }

            finish();



    }
}