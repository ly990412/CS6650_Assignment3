package org.example;

import java.util.List;

public class User {
    private int Id;
    private List<Integer> Users;

    public int getId() {
        return Id;
    }
    public void setId(int Id) {
        this.Id = Id;
    }

    public List<Integer> getUsers() {
        return Users;
    }
    public void setUsers(List<Integer> Users) { this.Users = Users; }
}
