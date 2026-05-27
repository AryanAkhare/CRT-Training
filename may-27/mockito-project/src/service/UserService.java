package service;

import repository.UserRepository;

public class UserService {

    private UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public String fetchName() {
        return repo.getUserName();
    }
}
