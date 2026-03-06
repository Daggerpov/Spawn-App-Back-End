package com.danielagapov.spawn.auth.internal.services;


import com.danielagapov.spawn.shared.util.OAuthProvider;

/**
 * Strategy interface for different OAuth providers
 */
public interface OAuthStrategy {

    OAuthProvider getOAuthProvider();

    String verifyIdToken(String idToken);
}
