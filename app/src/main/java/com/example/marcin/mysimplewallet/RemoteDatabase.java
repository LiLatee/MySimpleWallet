package com.example.marcin.mysimplewallet;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executor;


public class RemoteDatabase
{

    private FirebaseUser user;
    public ArrayList<Registry> registries;
    private String timestamp;

    public RemoteDatabase(FirebaseUser user)
    {
        this.user = user;
        this.timestamp = "0";
        registries = new ArrayList<Registry>();
        refresh();
    }

    public void addRegistry(Registry registry)
    {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        db.child(registry.id).setValue(registry);
        registries.add(registry);
        timestamp = registry.timestamp;
    }

    public void removeRegistry(Registry registry)
    {
        final String id = registry.id;
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        db.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

                for (DataSnapshot child : dataSnapshot.getChildren())
                {
                    Registry registry = child.getValue(Registry.class);
                    if (id.equals(registry.id))
                    {
                        child.getRef().removeValue();
                        break;
                    }
                }
                refresh();
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                //TODO
                //Toast.makeText(getBaseContext(), "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
            }
        });
        timestamp = Long.toString(new Date().getTime());

    }

    public void removeAll()
    {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        db.removeValue();
        timestamp = Long.toString(new Date().getTime());
    }

    public void refresh()
    {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        db.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

                for (DataSnapshot child : dataSnapshot.getChildren())
                {
                    Registry registry = child.getValue(Registry.class);
                    registries.add(registry);
                }
                refreshTimestamp();

            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                //TODO
                //Toast.makeText(getBaseContext(), "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    private void refreshTimestamp()
    {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        db.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

                for (DataSnapshot child : dataSnapshot.getChildren())
                {
                    Registry registry = child.getValue(Registry.class);
                    if (Long.parseLong(registry.timestamp) > Long.parseLong(timestamp))
                        timestamp = registry.timestamp;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                //TODO
                //Toast.makeText(getBaseContext(), "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public String generateKey()
    {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());

        return db.push().getKey();
    }
}
