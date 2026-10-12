package com.example.todo.repository;

import com.example.todo.entity.TodoItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Every method takes the owner's id, so there is no way to reach another user's items through this interface. */
public interface TodoItemRepository extends JpaRepository<TodoItem, UUID> {

    /** Active items first, then priority high to low, then oldest first; {@code id} keeps ties stable. */
    @Query("select t from TodoItem t where t.userId = :userId order by t.completed, t.priority, t.createdAt, t.id")
    List<TodoItem> findAllOrdered(@Param("userId") UUID userId);

    Optional<TodoItem> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TodoItem t where t.id = :id and t.userId = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
