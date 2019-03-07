package com.example.marcin.mysimplewallet;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

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
        Log.d("koy", "time " + timestamp);
    }

    public void removeRegistry(Registry registry)
    {
        final String id = registry.id;
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        db.addValueEventListener(new ValueEventListener()
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
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                //TODO
                //Toast.makeText(getBaseContext(), "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
            }
        });
        timestamp = Long.toString(new Date().getTime());
        refresh();
    }

    public void removeAll()
    {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        db.removeValue();
    }

    public void refresh()
    {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        db.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

                for (DataSnapshot child : dataSnapshot.getChildren())
                {
                    Registry registry = child.getValue(Registry.class);
                    registries.add(registry);
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

    public String getTimestamp()
    {
        return timestamp;
    }

    public String generateKey()
    {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());

        return db.push().getKey();
    }
}
