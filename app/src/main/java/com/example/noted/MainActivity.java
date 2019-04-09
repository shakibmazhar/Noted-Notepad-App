package com.example.noted;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ActionMode;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.noted.adapters.NotesAdapter;
import com.example.noted.db.NotesDB;
import com.example.noted.db.NotesDao;
import com.example.noted.model.Note;

import java.util.ArrayList;
import java.util.List;

import static com.example.noted.EditNoteActivity.NOTE_EXTRA_KEY;

public class MainActivity extends AppCompatActivity implements NoteEventListener{

    private RecyclerView recyclerView;
    private ArrayList<Note> notes;
    private NotesAdapter adapter;
    private NotesDao dao;
    private FloatingActionButton fab;
    private MainActionModeCallback actionModeCallback;
    private int checkedCount = 0;
    private MenuItem search;
    private String titleSearch;
    private String textSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        recyclerView = findViewById(R.id.notes_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onAddNewNote();
            }
        });
        dao = NotesDB.getInstance(this).notesDao();

    }

    private void loadNotes() {
        this.notes = new ArrayList<>();
        List<Note> list = dao.getNotes(); //Get notes from DB
        this.notes.addAll(list);
        this.adapter = new NotesAdapter(this, this.notes);
        this.adapter.setListener(this);
        this.recyclerView.setAdapter(adapter);
        showEmptyView();

        swipeToDeleteHelper.attachToRecyclerView(recyclerView);

    }
    private void showEmptyView() {
        if (notes.size() == 0) {
            this.recyclerView.setVisibility(View.GONE);
            findViewById(R.id.empty_notes_view).setVisibility(View.VISIBLE);

        } else {
            this.recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_notes_view).setVisibility(View.GONE);
        }
    }

    private void onAddNewNote() {
        startActivity(new Intent(this, EditNoteActivity.class));
        loadNotes();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        search = menu.findItem(R.id.action_search);
        if(notes.size()!= 0) {
            SearchView searchView = (SearchView) search.getActionView();
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    s = s.toLowerCase();
                    ArrayList<Note> searchList = new ArrayList<>();
                    for (Note note : notes) {
                        String titleSearch = note.getNoteTitle().toLowerCase();
                        String textSearch = note.getNoteText().toLowerCase();
                        if (titleSearch.contains(s)|| textSearch.contains(s)) {
                            searchList.add(note);
                        }
                    }
                    adapter.setFilter(searchList);
                    return true;
                }
            });
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    @Override
    public void onNoteClick(Note note) {
        Intent edit = new Intent(this, EditNoteActivity.class);
        edit.putExtra(NOTE_EXTRA_KEY, note.getId());
        startActivity(edit);

    }

    @Override
    public void onNoteLongClick(Note note) {
        note.setChecked(true);
        checkedCount = 1;
        adapter.setMultiCheckMode(true);


        adapter.setListener(new NoteEventListener() {
            @Override
            public void onNoteClick(Note note) {
                note.setChecked(!note.isChecked()); // inverse selected
                if (note.isChecked())
                    checkedCount++;
                else checkedCount--;

                if (checkedCount > 1) {
                    actionModeCallback.changeShareItemVisible(false);
                } else actionModeCallback.changeShareItemVisible(true);

                if(checkedCount == 0){
                    actionModeCallback.getAction().finish();
                }

                actionModeCallback.setCount(checkedCount + "/" + notes.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNoteLongClick(Note note) {

            }
        });

        actionModeCallback = new MainActionModeCallback() {
            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_delete_notes)
                    onDeleteMultiNotes();
                else if (menuItem.getItemId() == R.id.action_share_note)
                    onShareNote();

                actionMode.finish();
                return false;
            }

        };

        startActionMode(actionModeCallback);
        actionModeCallback.setCount(checkedCount + "/" + notes.size());
    }

    private void onShareNote() {
        Note note = adapter.getCheckedNotes().get(0);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        String noteText = "Title: " + note.getNoteTitle() +"\n" + note.getNoteText();
        share.putExtra(Intent.EXTRA_TEXT, noteText);
        startActivity(share);
    }

    private void onDeleteMultiNotes() {
        List<Note> checkedNotes = adapter.getCheckedNotes();
        if(checkedNotes.size()!=0){
            for(Note note : checkedNotes){
                dao.deleteNote(note);
            }
            loadNotes();
        }
        else Toast.makeText(this, "No notes selected", Toast.LENGTH_SHORT).show();
        
    }

    public void onActionModeFinished(ActionMode mode){
        super.onActionModeFinished(mode);
        adapter.setMultiCheckMode(false);
        adapter.setListener(this);
    }
    private ItemTouchHelper swipeToDeleteHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT ) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        if(notes!=null){
            Note swipedNote = notes.get(viewHolder.getAdapterPosition());
            if(swipedNote!=null){
                swipeToDelete(swipedNote, viewHolder);
            }

        }
        }
    });

    private void swipeToDelete(final Note swipedNote, final RecyclerView.ViewHolder viewHolder) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("Delete note?\nThis action cannot be reversed!")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    dao.deleteNote(swipedNote);
                    notes.remove(swipedNote);
                    adapter.notifyDataSetChanged();
                    adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                    loadNotes();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recyclerView.getAdapter().notifyItemChanged(viewHolder.getAdapterPosition());
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }

}
