package org.example;

import java.util.Set;
public class User {
    private int Id;
    private Set<Integer> Users;

    public int getId() {
        return Id;
    }
    public void setId(int Id) {
        this.Id = Id;
    }

    public Set<Integer> getUsers() {
        return Users;
    }
    public void setUsers(Set<Integer> Users) { this.Users = Users; }
}
