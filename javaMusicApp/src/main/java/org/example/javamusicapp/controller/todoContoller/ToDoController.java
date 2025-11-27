package org.example.javamusicapp.controller.todoContoller;

import org.example.javamusicapp.controller.todoContoller.dto.CreateToDoRequest;
import org.example.javamusicapp.controller.todoContoller.dto.ToDoResponse;
import org.example.javamusicapp.model.ToDo;
import org.example.javamusicapp.service.misc.ToDoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.example.javamusicapp.controller.todoContoller.dto.UpdateToDoRequest;

/**
 * ✅ **Was geht hier ab?**
 * Das ist ein klassischer Controller für eine To-Do-Liste. Jeder User, der eingeloggt ist,
 * kann hier seine eigenen To-Do-Items managen. Full-on self-organization.
 *
 * Die Endpunkte sind Standard-CRUD-Vibes:
 * - **POST /**: Erstellt ein neues To-Do für den eingeloggten User.
 * - **GET /me**: Listet alle To-Dos vom eingeloggten User auf.
 * - **DELETE /{id}**: Löscht ein To-Do, aber nur, wenn es dir auch gehört.
 * - **PATCH /{id}**: Updated ein To-Do teilweise (z.B. nur den Status ändern).
 * - **PUT /{id}**: Updated ein To-Do komplett.
 *
 * Wichtig: Man kann immer nur seine eigenen To-Dos sehen und bearbeiten. No peeking at other's lists!
 */
@RestController
@RequestMapping("/api/todos")
public class ToDoController {
    private final ToDoService toDoService;

    public ToDoController(ToDoService toDoService) {
        this.toDoService = toDoService;
    }

    @PostMapping
    @Operation(summary = "Create ToDo", description = "Creates a new ToDo owned by the authenticated user", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = CreateToDoRequest.class))), responses = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ToDoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Too Many Requests")
    })
    public ResponseEntity<ToDoResponse> createToDo(
            @org.springframework.web.bind.annotation.RequestBody CreateToDoRequest request, Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String username = principal.getName();
        ToDo toDo = new ToDo();
        toDo.setTitle(request.getTitle());
        toDo.setDescription(request.getDescription());
        toDo.setStatus(request.getStatus());
        ToDo created = toDoService.createToDo(toDo, username);

        ToDoResponse response = new ToDoResponse(
                created.getId(),
                created.getTitle(),
                created.getDescription(),
                created.getStatus(),
                created.getUser().getId(),
                created.getUser().getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @Operation(summary = "Get todos from current user", description = "Get todos from current user", responses = {
            @ApiResponse(responseCode = "201", description = "Get", content = @Content(schema = @Schema(implementation = ToDoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Too Many Requests")
    })
    public ResponseEntity<List<ToDoResponse>> listMyToDos(Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String username = principal.getName();
        List<ToDoResponse> result = toDoService.findByUserUsername(username).stream()
                .map(created -> new ToDoResponse(
                        created.getId(),
                        created.getTitle(),
                        created.getDescription(),
                        created.getStatus(),
                        created.getUser().getId(),
                        created.getUser().getUsername()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a ToDo of current user", description = "Deletes the ToDo with given id if it belongs to the authenticated user", responses = {
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not Found")
    })
    public ResponseEntity<Void> deleteToDo(@PathVariable("id") UUID id, Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String username = principal.getName();
        try {
            toDoService.deleteToDoIfOwner(id, username);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Patch ToDo", description = "Partially update a ToDo owned by the authenticated user", responses = {
            @ApiResponse(responseCode = "200", description = "Updated", content = @Content(schema = @Schema(implementation = ToDoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not Found")
    })
    public ResponseEntity<ToDoResponse> patchToDo(@PathVariable("id") UUID id,
            @org.springframework.web.bind.annotation.RequestBody UpdateToDoRequest request,
            Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String username = principal.getName();
        try {
            ToDo updated = toDoService.updateToDoIfOwner(id, request, username);
            ToDoResponse response = new ToDoResponse(
                    updated.getId(), updated.getTitle(), updated.getDescription(), updated.getStatus(),
                    updated.getUser().getId(), updated.getUser().getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    @Operation(summary = "Replace ToDo", description = "Fully replace a ToDo owned by the authenticated user", responses = {
            @ApiResponse(responseCode = "200", description = "Replaced", content = @Content(schema = @Schema(implementation = ToDoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not Found")
    })
    public ResponseEntity<ToDoResponse> replaceToDo(@PathVariable("id") UUID id,
            @org.springframework.web.bind.annotation.RequestBody CreateToDoRequest request,
            Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String username = principal.getName();
        try {
            ToDo replaced = toDoService.replaceToDoIfOwner(id, request, username);
            ToDoResponse response = new ToDoResponse(
                    replaced.getId(), replaced.getTitle(), replaced.getDescription(), replaced.getStatus(),
                    replaced.getUser().getId(), replaced.getUser().getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

}