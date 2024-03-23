package org.air.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;

import javax.persistence.*;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Refresh {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @ToString.Exclude // Exclude from toString()
    @EqualsAndHashCode.Exclude // Exclude from equals() and hashCode()
    private User user;

}