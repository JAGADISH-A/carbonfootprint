package com.carbonfootprint.platform.activity.port.out;

import com.carbonfootprint.platform.activity.model.Activity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port: persistence contract for {@link Activity} domain objects.
 *
 * <p>This interface is framework-independent — it contains no Firestore,
 * JPA, or Spring-specific types. Implementations may use Firestore,
 * PostgreSQL, or an in-memory store for testing.
 *
 * <p>Implementations:
 * {@link com.carbonfootprint.platform.activity.adapter.out.firestore.FirestoreActivityRepository}
 */
public interface ActivityRepository {

    /**
     * Persists a new activity or replaces an existing one with the same ID.
     *
     * @param activity the activity to save
     * @return the saved activity (may include server-generated fields)
     */
    Activity save(Activity activity);

    /**
     * Retrieves an activity by its unique identifier.
     *
     * @param id the activity ID
     * @return an {@link Optional} containing the activity, or empty if not found
     */
    Optional<Activity> findById(String id);

    /**
     * Retrieves all activities belonging to a user, ordered by {@code occurredAt} descending.
     *
     * @param userId the user identifier
     * @return list of activities (may be empty)
     */
    List<Activity> findByUserId(String userId);

    /**
     * Retrieves activities for a user within a time window.
     *
     * @param userId the user identifier
     * @param from   start of the window (inclusive)
     * @param to     end of the window (inclusive)
     * @return list of activities within the window
     */
    List<Activity> findByUserIdAndOccurredAtBetween(String userId, Instant from, Instant to);

    /**
     * Checks whether an activity with the same source, userId, and occurredAt
     * already exists. Used for duplicate detection.
     *
     * @param userId      the user identifier
     * @param rawDocumentId the raw document ID to check
     * @return {@code true} if a duplicate exists
     */
    boolean existsByUserIdAndRawDocumentId(String userId, String rawDocumentId);

    /**
     * Deletes an activity by its ID.
     *
     * @param id the activity ID
     */
    void deleteById(String id);
}
