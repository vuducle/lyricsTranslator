package org.example.javamusicapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "app_user")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({ "todos", "password", "authorities", "accountNonExpired", "accountNonLocked",
        "credentialsNonExpired", "enabled", "nachweiseAlsAzubi", "nachweiseAlsAusbilder" })
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String name;

    private Integer ausbildungsjahr;
    private String telefonnummer;
    private String team;

    @Column(nullable = false)
    private String password;
    private String email;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ToDo> todos = new ArrayList<>();

    @OneToMany(mappedBy = "azubi", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Nachweis> nachweiseAlsAzubi = new ArrayList<>();

    @OneToMany(mappedBy = "ausbilder", fetch = FetchType.LAZY)
    private List<Nachweis> nachweiseAlsAusbilder = new ArrayList<>();

    // 2. KORRIGIERTE getAuthorities() Methode
    // Gibt die Rollen aus der Datenbank zurück, nicht nur "ROLE_USER" statisch.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }
}
