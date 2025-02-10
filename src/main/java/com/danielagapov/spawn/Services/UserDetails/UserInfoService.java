package com.danielagapov.spawn.Services.UserDetails;

import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserInfo;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * This class is used to implement the UserDetailsService interface which Spring Security relies on
 * for authenticating requests
 */
@Service
public class UserInfoService implements UserDetailsService {
    private final IUserRepository repository;

    public UserInfoService(IUserRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieves user from repository by username and returns it as a UserDetails object
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repository.findByUsername(username);
        if (user == null) { throw new UsernameNotFoundException(username); }

        return new UserInfo(user.getUsername(), user.getPassword());
    }
}
