package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.service.CustomEntityService;
import com.example.demo.util.LocalDateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class MainController {
    @Autowired
    AppUserRepository userRepo;
    @Autowired
    CustomEntityService customEntityService;


    @GetMapping("/users/{id}")
    public Optional<AppUser> getUser(@PathVariable("id") long id, HttpServletRequest request) {

        return userRepo.findById(id);
    }

    private static final int testSize = 10000;

    @GetMapping("/write1")
    public String writeRecordsByBatch1() {
        List<AppUser> list = new ArrayList<>(testSize);
        for(int i=0; i<testSize; i++) {
            AppUser user = new AppUser();
            user.setId(null);
            user.setUserName("Random user 000000000" + i);
            list.add(user);
        }
       userRepo.saveAll(list);
        return "Done";
    }

    @GetMapping("/write")
    public String writeRecordsByBatch() {
        List<AppUser> list = new ArrayList<>(testSize);
        for(int i=0; i<testSize; i++) {
            AppUser user = new AppUser();
            user.setUserName("Random user 000000000" + i);
            user.setLastUpdateTime(LocalDateTimeUtil.getISOTime(LocalDateTime.now()));
            list.add(user);
        }
        customEntityService.batchInsertWithoutId(list);
        return "Done";
    }
}
