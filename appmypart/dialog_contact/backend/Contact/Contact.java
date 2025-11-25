package com.example.ridergurdianx;

public class Contact {

    private String id;        
    private String name;
    private String phone;
    private String relation;
    private String priority;

    
    public Contact() {
    }

    
    public Contact(String name, String phone, String relation, String priority) {
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.priority = priority;
    }

   
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
