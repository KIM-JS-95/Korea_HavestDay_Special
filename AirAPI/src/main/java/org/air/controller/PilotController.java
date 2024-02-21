package org.air.controller;

/**
 * Author : KIM JAE SEONG <br>
 * Content: 사용자가 일반적으로 사용 가능한 기능 모음집 <br>
 * Function <br>
 * index: 일정 호출 <br>
 * getTodaySchedules: 당일 모든 일정 획득 <br>
 * getDateSchedules : 요청일 스케줄 가져오기 <br>
 * showAllSchedules : 모든 스케쥴 가져오기 <br>
 * <br>
 */

import org.air.config.HeaderSetter;
import org.air.entity.Schedule;
import org.air.service.ScheduleService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@RestController
public class PilotController {

    @Autowired
    private ScheduleService scheduleService;

    // 메인 페이지
    @GetMapping("/home")
    public ResponseEntity index(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        HeaderSetter headerSetter = new HeaderSetter();
        HttpHeaders header = headerSetter.haederSet(token, "main page");
        return ResponseEntity.ok()
                .headers(header)
                .body("home");
    }

    @GetMapping("/getschedule_by_today") // 당일 스케쥴 가져오기 (복수 일정)
    public ResponseEntity getTodaySchedules(HttpServletRequest request) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMMyy", Locale.ENGLISH);
        String today = dateFormat.format(new Date());
        
        HeaderSetter headers = new HeaderSetter();
        HttpHeaders header = headers.haederSet(request.getHeader("Authorization"), "main page");
        List<Schedule> list = scheduleService.getSchedules(today);

        JSONObject obj = new JSONObject(); // json object 생성
        obj.put("schedules",list);

        return ResponseEntity.ok()
                .headers(header)
                .body(obj);
    }

    @GetMapping("/getschedule_by_date")
    public ResponseEntity getDateSchedules(@RequestParam("s_date") String sDate, @RequestParam("e_date") String eDate, HttpServletRequest request) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMMyy", Locale.ENGLISH);
        String startDate = sDate != null ? sDate : dateFormat.format(new Date());
        String endDate = eDate != null ? eDate : "";

        HeaderSetter headers = new HeaderSetter();
        HttpHeaders header = headers.haederSet(request.getHeader("Authorization"), "main page");
        List<Schedule> list = scheduleService.getSchedules(startDate, endDate);

        JSONObject obj = new JSONObject();
        obj.put("schedules", list);

        return ResponseEntity.ok()
                .headers(header)
                .body(obj);
    }




    @GetMapping("/show-schedule")
    public ResponseEntity showAllSchedules(HttpServletRequest request){
        String msg ="";
        List<Schedule> schedules = scheduleService.getALlSchedules();
        if(schedules.size()==0){
            msg="No Data";
        }else {
            msg="find All!";
        }

        HeaderSetter headerSetter = new HeaderSetter();
        HttpHeaders header =headerSetter.haederSet(request.getHeader("Authorization"),msg);

        return ResponseEntity.ok()
                .headers(header)
                .body(schedules);
    }
}
