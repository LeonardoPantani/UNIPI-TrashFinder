package it.unipi.di.pantani.trashfinder.data.loggedusers;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "loggedusers_table")
public class LoggedUser {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String username;

    @NonNull
    private String email;

    public LoggedUser(@NonNull String username, @NonNull String email) {
        this.username = username;
        this.email = email;
    }
}
