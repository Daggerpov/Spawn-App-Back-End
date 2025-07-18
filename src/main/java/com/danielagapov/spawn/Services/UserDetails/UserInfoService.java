package com.danielagapov.spawn.Services.UserDetails;

import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.User.UserInfo;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
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
        User user = repository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));

        // Return UserInfo with the correct password from the user entity
        return new UserInfo(user.getUsername(), user.getPassword());
    }
}
