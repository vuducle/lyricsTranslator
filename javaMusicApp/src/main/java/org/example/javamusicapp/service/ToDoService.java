package org.example.javamusicapp.service;

import org.example.javamusicapp.model.ToDo;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.repository.ToDoRepository;
import org.example.javamusicapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ToDoService {
    private final ToDoRepository toDoRepository;
    private final UserRepository userRepository;

    public ToDoService(ToDoRepository toDoRepository, UserRepository userRepository) {
        this.toDoRepository = toDoRepository;
        this.userRepository = userRepository;
    }

    public ToDo createToDo(ToDo toDo, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        toDo.setUser(user);
        return toDoRepository.save(toDo);
    }

    public ToDo findById(UUID id) {
        return toDoRepository.findById(id).orElse(null);
    }

    public List<ToDo> findByUserUsername(String username) {
        return toDoRepository.findByUserUsername(username);
    }

    public void deleteToDoIfOwner(UUID id, String username) {
        ToDo toDo = toDoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ToDo not found: " + id));
        if (toDo.getUser() == null || !username.equals(toDo.getUser().getUsername())) {
            throw new SecurityException("Not authorized to delete this ToDo");
        }
        toDoRepository.delete(toDo);
    }

    public ToDo updateToDoIfOwner(java.util.UUID id,
            org.example.javamusicapp.controller.todoContoller.dto.UpdateToDoRequest dto, String username) {
        ToDo toDo = toDoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ToDo not found: " + id));
        if (toDo.getUser() == null || !username.equals(toDo.getUser().getUsername())) {
            throw new SecurityException("Not authorized to update this ToDo");
        }

        if (dto.getTitle() != null)
            toDo.setTitle(dto.getTitle());
        if (dto.getDescription() != null)
            toDo.setDescription(dto.getDescription());
        if (dto.getStatus() != null)
            toDo.setStatus(dto.getStatus());

        return toDoRepository.save(toDo);
    }

    // Full replace: require all fields from the client (no partial updates)
    public ToDo replaceToDoIfOwner(java.util.UUID id,
            org.example.javamusicapp.controller.todoContoller.dto.CreateToDoRequest dto, String username) {
        ToDo toDo = toDoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ToDo not found: " + id));
        if (toDo.getUser() == null || !username.equals(toDo.getUser().getUsername())) {
            throw new SecurityException("Not authorized to replace this ToDo");
        }

        // Validate required fields for full replace
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }

        toDo.setTitle(dto.getTitle());
        toDo.setDescription(dto.getDescription());
        toDo.setStatus(dto.getStatus());

        return toDoRepository.save(toDo);
    }
}
