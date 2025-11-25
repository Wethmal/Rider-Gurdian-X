package com.example.ridergurdianx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SosContactsActivity extends AppCompatActivity
        implements ContactAdapter.OnContactClickListener {

    private RecyclerView rvContacts;
    private FloatingActionButton btnAddContact;
    private ContactAdapter adapter;
    private List<Contact> contactList = new ArrayList<>();

    
    private FirebaseFirestore db;
    private CollectionReference contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_contacts);

        
        db = FirebaseFirestore.getInstance();
        contactsRef = db.collection("sos_contacts");  

        rvContacts = findViewById(R.id.rvContacts);
        btnAddContact = findViewById(R.id.btnAddContact);

        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter(contactList, this);
        rvContacts.setAdapter(adapter);

        
        loadContactsFromFirebase();

        btnAddContact.setOnClickListener(v -> showAddEditDialog(null, -1));
    }

    
    private void loadContactsFromFirebase() {
        contactList.clear();
        contactsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Contact c = doc.toObject(Contact.class);
                        c.setId(doc.getId()); 
                        contactList.add(c);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading contacts: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

   
    private void showAddEditDialog(Contact contactToEdit, int position) {
        boolean isEdit = contactToEdit != null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Edit Contact" : "Add SOS Contact");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_contact, null);
        EditText etName = view.findViewById(R.id.etName);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etRelation = view.findViewById(R.id.etRelation);
        EditText etPriority = view.findViewById(R.id.etPriority);

        if (isEdit) {
            etName.setText(contactToEdit.getName());
            etPhone.setText(contactToEdit.getPhone());
            etRelation.setText(contactToEdit.getRelation());
            etPriority.setText(contactToEdit.getPriority());
        }

        builder.setView(view);

        builder.setPositiveButton(isEdit ? "Update" : "Save",
                (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String relation = etRelation.getText().toString().trim();
                    String priority = etPriority.getText().toString().trim();

                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(SosContactsActivity.this,
                                "Name and phone are required.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (priority.isEmpty()) priority = "High";

                    if (isEdit && position >= 0) {
                        
                        contactToEdit.setName(name);
                        contactToEdit.setPhone(phone);
                        contactToEdit.setRelation(relation);
                        contactToEdit.setPriority(priority);

                        contactsRef.document(contactToEdit.getId())
                                .set(contactToEdit)
                                .addOnSuccessListener(unused -> {
                                    adapter.notifyItemChanged(position);
                                    Toast.makeText(SosContactsActivity.this,
                                            "Contact updated",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(SosContactsActivity.this,
                                                "Update failed: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());

                    } else {
                        
                        Contact newContact =
                                new Contact(name, phone, relation, priority);

                        contactsRef.add(newContact)
                                .addOnSuccessListener(docRef -> {
                                    newContact.setId(docRef.getId());
                                    contactList.add(newContact);
                                    adapter.notifyItemInserted(contactList.size() - 1);
                                    Toast.makeText(SosContactsActivity.this,
                                            "Contact saved",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(SosContactsActivity.this,
                                                "Save failed: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    }
                });

        builder.setNegativeButton("Cancel",
                (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    
    @Override
    public void onContactClick(Contact contact, int position) {
        showAddEditDialog(contact, position);
    }

    
    @Override
    public void onContactLongClick(Contact contact, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete contact?")
                .setMessage("Remove " + contact.getName() + " from SOS contacts?")
                .setPositiveButton("Delete", (dialog, which) -> {

                    
                    contactsRef.document(contact.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                contactList.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(SosContactsActivity.this,
                                        "Deleted",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(SosContactsActivity.this,
                                            "Delete failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
