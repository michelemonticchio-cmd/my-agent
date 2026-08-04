package com.monticchio.myagent.service;

import com.monticchio.myagent.entity.User;
import com.monticchio.myagent.exception.QuotaExceededException;
import com.monticchio.myagent.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class QuotaService {

    private static final int FREE_REQUESTS_PER_WINDOW = 10;
    private static final Duration WINDOW_DURATION = Duration.ofHours(2);

    private final UserRepository userRepository;

    public QuotaService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Starts a fresh window (count 0, unlimited cleared) only if none is active yet or the
    // current one has expired; leaves an already-active window untouched either way.
    private void ensureActiveWindow(User user) {
        Instant now = Instant.now();
        if (user.getWindowStart() == null || now.isAfter(user.getWindowStart().plus(WINDOW_DURATION))) {
            user.setWindowStart(now);
            user.setRequestCount(0);
            user.setUnlimitedThisWindow(false);
        }
    }

    public void checkAndConsume(User user) {
        ensureActiveWindow(user);

        if (!user.isUnlimitedThisWindow() && user.getRequestCount() >= FREE_REQUESTS_PER_WINDOW) {
            throw new QuotaExceededException(
                    "Free quota of " + FREE_REQUESTS_PER_WINDOW + " requests per " + WINDOW_DURATION.toHours()
                            + " hours exceeded. Pay to unlock unlimited requests until the window resets.");
        }

        user.setRequestCount(user.getRequestCount() + 1);
        userRepository.save(user);
    }

    // Called after a confirmed payment: ensures a window exists (starting one now if needed)
    // so "unlimited for the current session" always refers to a concrete, already-running window.
    public void grantUnlimitedForCurrentWindow(User user) {
        ensureActiveWindow(user);
        user.setUnlimitedThisWindow(true);
        userRepository.save(user);
    }

    public record QuotaStatus(int used, int limit, boolean unlimited, Instant windowResetAt) {}

    public QuotaStatus getStatus(User user) {
        Instant now = Instant.now();
        boolean windowActive = user.getWindowStart() != null && !now.isAfter(user.getWindowStart().plus(WINDOW_DURATION));
        int used = windowActive ? user.getRequestCount() : 0;
        boolean unlimited = windowActive && user.isUnlimitedThisWindow();
        Instant resetAt = windowActive ? user.getWindowStart().plus(WINDOW_DURATION) : null;
        return new QuotaStatus(used, FREE_REQUESTS_PER_WINDOW, unlimited, resetAt);
    }
}
