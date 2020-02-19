package com.fpt.edu.schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class UserName {

    @Id
    private String id;
    private String fullName;
    private String email;
    @ManyToMany()
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @JsonIgnore
    private List<Role> roleList;

    @ManyToMany()
    @JoinTable(name = "expected_subject",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @JsonIgnore
    private List<Subject> expectedSubject;


    @ManyToMany()
    @JoinTable(name = "expected_slot",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "slot_id")
    )
    @JsonIgnore
    private List<Slot> expectedSlot;

    @OneToOne(mappedBy = "userName")
    private ExpectedNote expectedNote;

}