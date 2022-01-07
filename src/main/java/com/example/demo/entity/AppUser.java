package com.example.demo.entity;

import com.example.demo.util.LocalDateTimeConverter;
import lombok.Data;
import org.springframework.data.domain.Persistable;

import javax.persistence.*;

@Entity
@Table(name = "test")
@Data
public class AppUser implements Persistable {

    /**
     * If you are using SEQUENCE, please do not generate with SEQUENCE, because it will try to query database to get the latest sequence. Use the IDENTITY would be quicker.
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String userName;

    private String myPassword;
    private String password;

    @Convert(converter = LocalDateTimeConverter.class)
    private String lastUpdateTime;

    @Override
    public boolean isNew() {
        return true;
    }

}
